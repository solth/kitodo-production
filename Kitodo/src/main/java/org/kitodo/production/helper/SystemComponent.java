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

/**
 * The class SystemComponent is a helper class to represent a component of the system with its name, version,
 * health status, and configuration status.
 */
public class SystemComponent {

    private String componentName;
    private String componentVersion = "N/A";
    private String componentHealth = "N/A";
    private boolean configured = true;

    public SystemComponent(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Get componentName.
     *
     * @return value of componentName
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Set componentName.
     *
     * @param componentName as java.lang.String
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Get componentVersion.
     *
     * @return value of componentVersion
     */
    public String getComponentVersion() {
        return componentVersion;
    }

    /**
     * Set componentVersion.
     *
     * @param componentVersion as java.lang.String
     */
    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    /**
     * Get componentHealth.
     *
     * @return value of componentHealth
     */
    public String getComponentHealth() {
        return componentHealth;
    }

    /**
     * Set componentHealth.
     *
     * @param componentHealth as java.lang.String
     */
    public void setComponentHealth(String componentHealth) {
        this.componentHealth = componentHealth;
    }

    /**
     * Get configured.
     *
     * @return value of configured
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Set configured.
     *
     * @param configured as boolean
     */
    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
}
