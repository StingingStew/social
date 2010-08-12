/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.application;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.RelationshipEvent;
import org.exoplatform.social.core.relationship.RelationshipEvent.Type;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.test.AbstractCoreTest;

public class RelationshipPublisherTest extends  AbstractCoreTest {
  private final Log LOG = ExoLogger.getLogger(RelationshipPublisher.class);
  private ActivityManager activityManager;
  private IdentityManager identityManager;
  private RelationshipManager relationshipManager;
  private RelationshipPublisher relationshipPublisher;
  private List<Activity> tearDownActivityList;
  public void setUp() throws Exception {
    super.setUp();
    tearDownActivityList = new ArrayList<Activity>();
    activityManager = (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
    assertNotNull("activityManager must not be null", activityManager);
    identityManager =  (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
    assertNotNull("identityManager must not be null", identityManager);
    relationshipManager =  (RelationshipManager) getContainer().getComponentInstanceOfType(RelationshipManager.class);
    assertNotNull("relationshipManager must not be null", relationshipManager);
    relationshipPublisher = (RelationshipPublisher) getContainer().getComponentInstanceOfType(RelationshipPublisher.class);
    assertNotNull("relationshipPublisher must not be null", relationshipPublisher);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    for (Activity activity : tearDownActivityList) {
      try {
        activityManager.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }
  }

  public void testConfirmed() throws Exception {
    assertTrue(true);

    Identity mary = identityManager.getIdentity("organization:mary");
    Identity john = identityManager.getIdentity("organization:john");
    RelationshipEvent event = new RelationshipEvent(Type.CONFIRM, relationshipManager, new Relationship(mary, john));
    relationshipPublisher.confirmed(event);
    List<Activity> maryActivities = activityManager.getActivities(mary);

    assertEquals(1, maryActivities.size());
    tearDownActivityList.add(maryActivities.get(0));
    assertTrue(maryActivities.get(0).getTitleId().equals("CONNECTION_CONFIRMED"));
    assertTrue(maryActivities.get(0).getTemplateParams().get("Requester").contains("mary"));
    assertTrue(maryActivities.get(0).getTemplateParams().get("Accepter").contains("john"));

    List<Activity> johnActivities = activityManager.getActivities(john);
    assertEquals(1, johnActivities.size());
    tearDownActivityList.add(johnActivities.get(0));
    assertTrue(johnActivities.get(0).getTitleId().equals("CONNECTION_CONFIRMED"));
    assertTrue(johnActivities.get(0).getTemplateParams().get("Requester").contains("john"));
    assertTrue(johnActivities.get(0).getTemplateParams().get("Accepter").contains("mary"));

  }
}