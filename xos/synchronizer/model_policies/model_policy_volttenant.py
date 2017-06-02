from synchronizers.new_base.modelaccessor import *
from synchronizers.new_base.policy import Policy

class VOLTTenantPolicy(Policy):
    model_name = "VOLTTenant"

    def handle_create(self, tenant):
        return self.handle_update(tenant)

    def handle_update(self, tenant):
        self.manage_vsg(tenant)
        self.manage_subscriber(tenant)
        self.cleanup_orphans(tenant)

    def handle_delete(self, tenant):
        if tenant.vcpe:
            tenant.vcpe.delete()

    def manage_vsg(self, tenant):
        # Each VOLT object owns exactly one VCPE object

        if tenant.deleted:
            self.logger.info("MODEL_POLICY: volttenant %s deleted, deleting vsg" % tenant)
            return

        # Check to see if the wrong s-tag is set. This can only happen if the
        # user changed the s-tag after the VoltTenant object was created.
        if tenant.vcpe and tenant.vcpe.instance:
            s_tags = Tag.objects.filter(content_type=tenant.vcpe.instance.self_content_type_id,
                                        object_id=tenant.vcpe.instance.id, name="s_tag")
            if s_tags and (s_tags[0].value != str(tenant.s_tag)):
                self.logger.info("MODEL_POLICY: volttenant %s s_tag changed, deleting vsg" % tenant)
                tenant.vcpe.delete()

        if tenant.vcpe is None:
            vsgServices = VSGService.objects.all()
            if not vsgServices:
                raise XOSConfigurationError("No VSG Services available")

            self.logger.info("MODEL_POLICY: volttenant %s creating vsg" % tenant)

            vcpe = VSGTenant(provider_service=vsgServices[0],
                             subscriber_tenant=tenant)
            vcpe.creator = tenant.creator
            vcpe.save()

    def manage_subscriber(self, tenant):
        if (tenant.subscriber_root is None):
            # The vOLT is not connected to a Subscriber, so either find an
            # existing subscriber with the same SSID, or autogenerate a new
            # subscriber.
            #
            # TODO: This probably goes away when we rethink the ONOS-to-XOS
            # vOLT API.

            subs = CordSubscriberRoot.objects.filter(service_specific_id = tenant.service_specific_id)
            if subs:
                self.logger.info("MODEL_POLICY: volttenant %s using existing subscriber root" % tenant)
                sub = subs[0]
            else:
                self.logger.info("MODEL_POLICY: volttenant %s creating new subscriber root" % tenant)
                sub = CordSubscriberRoot(service_specific_id = tenant.service_specific_id,
                                         name = "autogenerated-for-vOLT-%s" % tenant.id)
                sub.save()
            tenant.subscriber_root = sub
            tenant.save()

    def cleanup_orphans(self, tenant):
        # ensure vOLT only has one vCPE
        cur_vcpe = tenant.vcpe
        subscribed_vcpes = VSGTenant.objects.filter(subscriber_tenant_id = tenant.id)
        for vcpe in subscribed_vcpes:
            if (not cur_vcpe) or (vcpe.id != cur_vcpe.id):
                vcpe.delete()
