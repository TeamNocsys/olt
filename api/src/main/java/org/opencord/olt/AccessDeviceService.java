/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencord.olt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.onlab.packet.VlanId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.opencord.sadis.SubscriberAndDeviceInformation;

import com.google.common.collect.ImmutableMap;

/**
 * Service for interacting with an access device (OLT).
 */
public interface AccessDeviceService
        extends ListenerService<AccessDeviceEvent, AccessDeviceListener> {

    /**
     * Provisions connectivity for a subscriber on an access device.
     *
     * @param port subscriber's connection point
     * @return true if successful false otherwise
     */
    boolean provisionSubscriber(ConnectPoint port);

    /**
     * Removes provisioned connectivity for a subscriber from an access device.
     *
     * @param port subscriber's connection point
     * @return true if successful false otherwise
     */
    boolean removeSubscriber(ConnectPoint port);

    /**
     * Provisions flows for the specific subscriber.
     *
     * @param subscriberId Identification of the subscriber
     * @return true if successful false otherwise
     */
    boolean provisionSubscriber(AccessSubscriberId subscriberId);

    /**
     * Removes flows for the specific subscriber.
     *
     * @param subscriberId Identification of the subscriber
     * @return true if successful false otherwise
     */
    boolean removeSubscriber(AccessSubscriberId subscriberId);

    /**
     * Returns information about the provisioned subscribers.
     *
     * @return subscribers
     */
    Collection<Map.Entry<ConnectPoint, Map.Entry<VlanId, VlanId>>> getSubscribers();

    /**
     * Returns the list of active OLTs.
     *
     * @return a List
     */
    List<DeviceId> fetchOlts();

    /**
     * Returns information about subscribers that have been programmed in the
     * dataplane.
     *
     * @return an immutable map of locations and subscriber information
     */
    ImmutableMap<ConnectPoint, SubscriberAndDeviceInformation> getProgSubs();

}
