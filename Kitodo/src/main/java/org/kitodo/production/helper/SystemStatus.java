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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import org.hibernate.Session;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.KitodoConfig;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.persistence.HibernateUtil;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.index.IndexingService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;


@Named("SystemStatus")
@ApplicationScoped
public class SystemStatus {

    private final Map<String, String> components = new HashMap<>();
    IndexingService indexingService = ServiceManager.getIndexingService();
    String databaseVersion;
    private final Path kitodoDataDirectory = Path.of(KitodoConfig.getKitodoDataDirectory());

    /**
     * Get components.
     *
     * @return value of components
     */
    public List<Map.Entry<String, String>> getComponents() {
        if (components.isEmpty()) {
            checkComponentStatus();
        }
        return components.entrySet().stream().toList();
    }

    /**
     * Update the status of system components.
     */
    public void updateStatus() {
        checkComponentStatus();
    }

    // TODO: move private methods and business logic to service classes

    private void checkComponentStatus() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        databaseVersion = null;
        components.clear();
        if (Objects.nonNull(facesContext)) {
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            components.put(Helper.getTranslation("status.database"), getDatabaseVersion());
            components.put(Helper.getTranslation("status.server"), servletContext.getServerInfo());
            components.put(Helper.getTranslation("status.searchServer"), indexingService.getServerVersion());
            components.put(Helper.getTranslation("status.fileSystem"), getFileSystemType());
            components.put(Helper.getTranslation("status.diskUsage"), getDiskUsage());
            components.put(Helper.getTranslation("status.activeMq"), getActiveMqInformation());
        }
    }

    private String getDiskUsage() {
        File kitodoDirectory = new File(KitodoConfig.getKitodoDataDirectory());
        String freeSpace = String.format("%.2f", kitodoDirectory.getFreeSpace() / 1073741824.0);
        String totalSpace = String.format("%.2f GB", kitodoDirectory.getTotalSpace() / 1073741824.0);
        return freeSpace + " / " + totalSpace;
    }

    private String getFileSystemType() {
        try {
            return Files.getFileStore(kitodoDataDirectory).type();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private String getDatabaseVersion() {
        if (Objects.isNull(databaseVersion)) {
            try (Session session = HibernateUtil.getSession()) {
                session.doWork(connection -> {
                    DatabaseMetaData databaseMetaData = connection.getMetaData();
                    databaseVersion = databaseMetaData.getDatabaseProductName() + " - " + databaseMetaData.getDatabaseProductVersion();
                });
            }
        }
        return databaseVersion;
    }

    private String getActiveMqInformation() {
        String activeMqHost;
        try {
            return ConfigCore.getParameter(ParameterCore.ACTIVE_MQ_HOST_URL);
        } catch (NoSuchElementException e) {
            activeMqHost = "not configured";
        }
        return activeMqHost;
    }
}
