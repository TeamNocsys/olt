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
package org.opencord.olt.impl;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.opencord.sadis.BandwidthProfileInformation;
import org.opencord.sadis.BaseInformationService;
import org.opencord.sadis.SubscriberAndDeviceInformation;
import org.opencord.sadis.SadisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OltTest {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Olt olt;

    private static final VlanId CLIENT_C_TAG = VlanId.vlanId((short) 999);
    private static final VlanId CLIENT_S_TAG = VlanId.vlanId((short) 111);
    private static final String CLIENT_NAS_PORT_ID = "PON 1/1";
    private static final String CLIENT_CIRCUIT_ID = "CIR-PON 1/1";

    private static final String OLT_DEV_ID = "of:00000000000000aa";
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId(OLT_DEV_ID);
    private static final String SCHEME_NAME = "olt";
    private static final DefaultAnnotations DEVICE_ANNOTATIONS = DefaultAnnotations.builder()
            .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase()).build();

    @Before
    public void setUp() {
        olt = new Olt();
        olt.deviceService = new MockDeviceService();
        olt.sadisService = new MockSadisService();
        olt.subsService = olt.sadisService.getSubscriberInfoService();

    }

    /**
     * Tests that the getSubscriber method does throw a NullPointerException with a meaningful message.
     */
    @Test
    public void testGetSubscriberError() {
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(OLT_DEV_ID + "/" + 1);
        try {
            olt.getSubscriber(cp);
        } catch (NullPointerException e) {
            assertEquals(e.getMessage(), "Invalid connect point");
        }
    }

    /**
     * Tests that the getSubscriber method returns Subscriber informations.
     */
    @Test
    public void testGetSubscriber() {
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(OLT_DEV_ID + "/" + 2);

        SubscriberAndDeviceInformation s =  olt.getSubscriber(cp);

        assertEquals(s.circuitId(), CLIENT_CIRCUIT_ID);
        assertEquals(s.cTag(), CLIENT_C_TAG);
        assertEquals(s.sTag(), CLIENT_S_TAG);
        assertEquals(s.nasPortId(), CLIENT_NAS_PORT_ID);
    }

    private class MockDevice extends DefaultDevice {

        public MockDevice(ProviderId providerId, DeviceId id, Type type,
                          String manufacturer, String hwVersion, String swVersion,
                          String serialNumber, ChassisId chassisId, Annotations... annotations) {
            super(providerId, id, type, manufacturer, hwVersion, swVersion, serialNumber,
                  chassisId, annotations);
        }
    }

    private class MockDeviceService extends DeviceServiceAdapter {

        private ProviderId providerId = new ProviderId("of", "foo");
        private final Device device1 = new MockDevice(providerId, DEVICE_ID_1, Device.Type.SWITCH,
                                                      "foo.inc", "0", "0", OLT_DEV_ID, new ChassisId(),
                                                      DEVICE_ANNOTATIONS);

        @Override
        public Device getDevice(DeviceId devId) {
            return device1;

        }

        @Override
        public Port getPort(ConnectPoint cp) {
            log.info("Looking up port {}", cp.port().toString());
            if (cp.port().toString().equals("1")) {
                return null;
            }
            return new MockPort();
        }
    }

    private class  MockPort implements Port {

        @Override
        public boolean isEnabled() {
            return true;
        }
        @Override
        public long portSpeed() {
            return 1000;
        }
        @Override
        public Element element() {
            return null;
        }
        @Override
        public PortNumber number() {
            return null;
        }
        @Override
        public Annotations annotations() {
            return new MockAnnotations();
        }
        @Override
        public Type type() {
            return Port.Type.FIBER;
        }

        private class MockAnnotations implements Annotations {

            @Override
            public String value(String val) {
                return "BRCM12345678";
            }
            @Override
            public Set<String> keys() {
                return null;
            }
        }
    }

    private class MockSadisService implements SadisService {


        @Override
        public BaseInformationService<SubscriberAndDeviceInformation> getSubscriberInfoService() {
            return new MockSubService();
        }

        @Override
        public BaseInformationService<BandwidthProfileInformation> getBandwidthProfileService() {
            return null;
        }
    }

    private class MockSubService implements BaseInformationService<SubscriberAndDeviceInformation> {
        MockSubscriberAndDeviceInformation sub =
                new MockSubscriberAndDeviceInformation(CLIENT_NAS_PORT_ID, CLIENT_C_TAG,
                                                       CLIENT_S_TAG, CLIENT_NAS_PORT_ID, CLIENT_CIRCUIT_ID, null, null);
        @Override
        public SubscriberAndDeviceInformation get(String id) {
            return  sub;
        }

        @Override
        public void invalidateAll() {}
        @Override
        public void invalidateId(String id) {}
        @Override
        public SubscriberAndDeviceInformation getfromCache(String id) {
            return null;
        }
    }

    private class MockSubscriberAndDeviceInformation extends SubscriberAndDeviceInformation {

        MockSubscriberAndDeviceInformation(String id, VlanId ctag,
                                           VlanId stag, String nasPortId,
                                           String circuitId, MacAddress hardId,
                                           Ip4Address ipAddress) {
            this.setCTag(ctag);
            this.setHardwareIdentifier(hardId);
            this.setId(id);
            this.setIPAddress(ipAddress);
            this.setSTag(stag);
            this.setNasPortId(nasPortId);
            this.setCircuitId(circuitId);
        }
    }

}