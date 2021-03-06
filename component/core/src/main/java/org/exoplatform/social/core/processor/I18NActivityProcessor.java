/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.common.ResourceBundleUtil;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Special activity processor to process i18n activity (which has titleId != null). This class will process any i18n
 * activity to dynamic new activity with i18n title.
 * <p/>
 * How to I18N-ize an activity:
 * <ul>
 *   <li>
 *     must set <em>titleId</em> to indicate it's an I18N activity. <em>titleId</em> is used to map with a specific message
 *     bundle key via configuration.
 *   </li>
 *   <li>
 *     if that resource bundle message is a compound resource bundle message, provide templateParams. The argument number will
 *     be counted as it appears on the map.
 *     For example: templateParams = {"key1": "value1", "key2": "value2"} => message bundle arguments = ["value1", "value2"].
 *     Note: to reserve the order of elements, LinkedHashMap must be used to create templateParams.
 *   </li>
 *   <li>
 *     create a resource bundle file and this file name is called "resourceBundleKeyFile" for configuration later.
 *   </li>
 *   <li>
 *     register that resource bundle file with {@link org.exoplatform.services.resources.impl.BaseResourceBundlePlugin} to
 *     {@link ResourceBundleService}.
 *   </li>
 *   <li>
 *     register {@link ActivityResourceBundlePlugin} with this service.
 *   </li>
 * </ul>
 *
 * @author <a href="http://hoatle.net">hoatle (hoatlevan at gmail dot com)</a>
 * @since 1.2.8
 * @since Feb 1, 2012
 * @see {@link ActivityResourceBundlePlugin}
 */
public final class I18NActivityProcessor {

  /**
   * The Logger.
   */
  private static final Log LOG = ExoLogger.getLogger(I18NActivityProcessor.class);

  /**
   * The map of registered resource bundle plugins. The key is activityType.
   */
  private Map<String, ActivityResourceBundlePlugin> resourceBundlePluginMap;

  /**
   * The resource bundle service for getting associated {@link ResourceBundle}.
   */
  private ResourceBundleService resourceBundleService;

  /**
   * Constructor.
   */
  public I18NActivityProcessor() {

  }

  /**
   * Registers an activity resource bundle plugin.
   *
   * @param activityResourceBundlePlugin the activity resource bundle plugin.
   */
  public void addActivityResourceBundlePlugin(ActivityResourceBundlePlugin activityResourceBundlePlugin) {
    //this could be a bug from exojcr as component plugin is not set name on it's constructor.
    activityResourceBundlePlugin.setActivityType(activityResourceBundlePlugin.getName());

    if (!activityResourceBundlePlugin.isValid()) {
      LOG.warn("Failed to register the plugin: not valid");
      return;
    }
    if (resourceBundlePluginMap == null) {
      resourceBundlePluginMap = new HashMap<String, ActivityResourceBundlePlugin>();
    }
    resourceBundlePluginMap.put(activityResourceBundlePlugin.getActivityType(),
            activityResourceBundlePlugin);
  }

  /**
   * Un-registers an existing registered resource bundle plugin.
   *
   * @param activityResourceBundlePlugin the existing activity resource bundle plugin.
   */
  public void removeActivityResourceBundlePlugin(ActivityResourceBundlePlugin activityResourceBundlePlugin) {
    if (!activityResourceBundlePlugin.isValid()) {
      LOG.warn("Failed to remove the plugin: not valid");
    }
    if (resourceBundlePluginMap == null) {
      LOG.info("resourceBundlePluginMap is null.");
      return;
    }
    resourceBundlePluginMap.remove(activityResourceBundlePlugin.getActivityType());
  }

  /**
   * Processes the I18N activity which means that activity.getTitleId() != null.
   *
   * @param i18nActivity the I18N activity
   * @param selectedLocale the selected locale
   *
   * @return the new activity with I18N title
   */
  public ExoSocialActivity process(ExoSocialActivity i18nActivity, Locale selectedLocale) {
    //only processes I18N activity type
    if (i18nActivity.getTitleId() != null) {
      if (activityTypeRegistered(i18nActivity)) {
        ResourceBundle resourceBundle = getResourceBundle(i18nActivity, selectedLocale);
        if (resourceBundle == null) {
          LOG.warn("no resource bundle key found registered for: " + getResourceBundleKeyFile(i18nActivity));
          return i18nActivity;
        }
        if (getMessageBundleKey(i18nActivity) == null) {
          LOG.warn("Failed to find registered message bundle key for titleId: " + i18nActivity.getTitleId());
          return i18nActivity;
        }
        String newTitle = appRes(resourceBundle, getMessageBundleKey(i18nActivity), i18nActivity.getTemplateParams());
        if (newTitle != null) {
          i18nActivity.setTitle(newTitle);
        }
      }
    }
    return i18nActivity;
  }

  /**
   * Sets the external resource bundle service.
   *
   * @param resourceBundleService the resource bundle service
   */
  public void setResourceBundleService(ResourceBundleService resourceBundleService) {
    this.resourceBundleService = resourceBundleService;
  }

  /**
   * Checks if this i18n activity has registered activity resource bundle plugin.
   *
   * @param i18nActivity the i18n activity.
   * @return a boolean value.
   */
  private boolean activityTypeRegistered(ExoSocialActivity i18nActivity) {
    if (resourceBundlePluginMap == null) {
      return false;
    }
    return resourceBundlePluginMap.containsKey(i18nActivity.getType());
  }

  /**
   * Gets the associated registered message bundle key for this i18n activity's titleId.
   *
   * @param i18nActivity the i18n activity.
   * @return the found registered message bundle key.
   */
  private String getMessageBundleKey(ExoSocialActivity i18nActivity) {
    ActivityResourceBundlePlugin resourceBundlePlugin = resourceBundlePluginMap.get(i18nActivity.getType());
    return resourceBundlePlugin.getMessageBundleKey(i18nActivity.getTitleId());
  }

  /**
   * Gets the associated resource bundle key file registered with this type of activity.
   *
   * @param i18nActivity the i18n activity
   * @return the associated resource bundle key file
   */
  private String getResourceBundleKeyFile(ExoSocialActivity i18nActivity) {
    return resourceBundlePluginMap.get(i18nActivity.getType()).getResourceBundleKeyFile();
  }

  /**
   * Gets the associated message bundle value from a specific message bundle key. If templateParams is not null, try to resolve
   * the detected compound message bundle.
   *
   * @param resourceBundle the registered resource bundle from resource bundle key file
   * @param msgKey         the message key
   * @param templateParams the possible template params for resolving compound message bundle.
   * @return the found message bundle value or null if not found.
   */
  private String appRes(ResourceBundle resourceBundle, String msgKey, Map<String, String> templateParams) {

    String value = appRes(resourceBundle, msgKey);
    List<String> arguments = null;
    if (templateParams != null) {
      arguments = new ArrayList(templateParams.values());
    }
    if (arguments != null && arguments.size() > 0) {
      value = ResourceBundleUtil.replaceArguments(value, arguments);
    }
    return value;
  }

  /**
   * Finds the message bundle value from message bundle key.
   *
   * @param res    the resource bundle.
   * @param msgKey the message bundle key.
   * @return the found message bundle value or null if not found.
   */
  private String appRes(ResourceBundle res, String msgKey) {
    String value = null;
    try {
      value = res.getString(msgKey);
    } catch (MissingResourceException ex) {
      LOG.warn("Failed find message bundle value for key : " + msgKey);
    }
    return value;
  }


  /**
   * Gets the associated registered resource bundle from an i18n activity and the selected locale.
   *
   * @param i18nActivity   the i18n activity.
   * @param selectedLocale the selected locale.
   * @return the associated registered resource bundle.
   */
  private ResourceBundle getResourceBundle(ExoSocialActivity i18nActivity, Locale selectedLocale) {
    if (resourceBundleService == null) {
      resourceBundleService = (ResourceBundleService) PortalContainer.getInstance().
              getComponentInstanceOfType(ResourceBundleService.class);
    }
    if (resourceBundleService == null) {
      LOG.error("Failed to get resourceBundleService for I18N activity type.");
      return null;
    }
    if (resourceBundlePluginMap == null || resourceBundlePluginMap.size() == 0) {
      LOG.warn("No registered activity resource bundle");
      return null;
    }
    ActivityResourceBundlePlugin resourceBundlePlugin = resourceBundlePluginMap.get(i18nActivity.getType());
    return resourceBundleService.getResourceBundle(resourceBundlePlugin.getResourceBundleKeyFile(), selectedLocale);
  }

}
