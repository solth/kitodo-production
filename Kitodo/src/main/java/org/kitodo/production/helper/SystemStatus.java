/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

import org.hibernate.Session;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.KitodoConfig;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.persistence.HibernateUtil;
import org.kitodo.production.interfaces.activemq.ActiveMQDirector;
import org.kitodo.production.services.ServiceManager;

/**
 * This class is used to check the status of system components and provide information about them.
 */
@Named("SystemStatus")
@ApplicationScoped
public class SystemStatus {

    private final LinkedList<SystemComponent> components = new LinkedList<>();
    private final Path kitodoDataDirectory = Path.of(KitodoConfig.getKitodoDataDirectory());

    /**
     * Get components.
     *
     * @return value of components
     */
    public List<SystemComponent> getComponents() {
        if (components.isEmpty()) {
            checkComponentStatus();
        }
        return components;
    }

    /**
     * Update the status of system components.
     */
    public void updateStatus() {
        checkComponentStatus();
    }

    // TODO: move private methods and business logic to service classes

    private void checkComponentStatus() {
        components.clear();
        components.add(getWebServerInformation());
        components.add(getDatabaseInformation());
        components.add(getSearchServerInformation());
        components.add(getFileSystemInformation());
        components.add(getActiveMqInformation());
        components.add(getLdapInformation());
    }

    private SystemComponent getWebServerInformation() {
        SystemComponent webServerComponent = new SystemComponent(Helper.getTranslation("status.server"));
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (Objects.nonNull(facesContext)) {
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            webServerComponent.setComponentVersion(servletContext.getServerInfo());
        }
        return webServerComponent;
    }

    private SystemComponent getFileSystemInformation() {
        SystemComponent diskUsageComponent = new SystemComponent(Helper.getTranslation("status.fileSystem"));
        File kitodoDirectory = new File(KitodoConfig.getKitodoDataDirectory());
        String freeSpace = String.format("%.2f", kitodoDirectory.getFreeSpace() / 1073741824.0);
        String totalSpace = String.format("%.2f GB", kitodoDirectory.getTotalSpace() / 1073741824.0);
        diskUsageComponent.setComponentVersion(getFileSystemType());
        diskUsageComponent.setComponentHealth(freeSpace + " / " + totalSpace + " " + Helper.getTranslation("status.diskUsage"));
        return diskUsageComponent;
    }

    private String getFileSystemType() {
        try {
            return Files.getFileStore(kitodoDataDirectory).type();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private SystemComponent getDatabaseInformation() {
        SystemComponent databaseComponent = new SystemComponent(Helper.getTranslation("status.database"));
        try (Session session = HibernateUtil.getSession()) {
            session.doWork(connection -> {
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                databaseComponent.setComponentVersion(databaseMetaData.getDatabaseProductName() + " - "
                        + databaseMetaData.getDatabaseProductVersion());
            });
        }
        return databaseComponent;
    }

    private SystemComponent getSearchServerInformation() {
        SystemComponent searchServerComponent = new SystemComponent(Helper.getTranslation("status.searchServer"));
        searchServerComponent.setComponentVersion(ServiceManager.getIndexingService().getServerVersion());
        searchServerComponent.setComponentHealth(ServiceManager.getIndexingService().getServerHealth());
        return searchServerComponent;
    }

    // Optional system components: checks, whether they are configured at all or not

    private SystemComponent getActiveMqInformation() {
        SystemComponent activeMqComponent = new SystemComponent(Helper.getTranslation("status.activeMq"));
        if (ConfigCore.getOptionalString(ParameterCore.ACTIVE_MQ_HOST_URL).isEmpty()) {
            activeMqComponent.setConfigured(false);
        } else {
            activeMqComponent.setComponentVersion(ActiveMQDirector.getActiveMqVersion());
        }
        return activeMqComponent;
    }

    private SystemComponent getLdapInformation() {
        SystemComponent ldapComponent = new SystemComponent(Helper.getTranslation("status.ldap"));
        if (ConfigCore.getOptionalString(ParameterCore.LDAP_USE).isEmpty()) {
            ldapComponent.setConfigured(false);
        }
        return ldapComponent;
    }
}
