package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.HostDevice;
import org.ovirt.engine.api.model.HostDevices;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.resource.VmHostDeviceResource;
import org.ovirt.engine.api.resource.VmHostDevicesResource;
import org.ovirt.engine.api.restapi.utils.HexUtils;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmHostDevicesParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

import javax.ws.rs.core.Response;
import java.util.List;

public class BackendVmHostDevicesResource
        extends AbstractBackendCollectionResource<HostDevice, org.ovirt.engine.core.common.businessentities.HostDevice>
        implements VmHostDevicesResource {

    private Guid vmId;

    public BackendVmHostDevicesResource(Guid parentId) {
        super(HostDevice.class, org.ovirt.engine.core.common.businessentities.HostDevice.class);
        vmId = parentId;
    }

    @Override
    protected HostDevice addParents(HostDevice model) {
        model.setVm(new VM());
        model.getVm().setId(vmId.toString());
        return super.addParents(model);
    }

    @Override
    public HostDevices list() {
        HostDevices model = new HostDevices();
        for (org.ovirt.engine.core.common.businessentities.HostDevice hostDevice : getCollection()) {
            model.getHostDevices().add(addLinks(map(hostDevice, new HostDevice())));
        }

        return model;
    }

    @Override
    public Response add(final HostDevice hostDevice) {
        validateParameters(hostDevice, "id|name");

        String deviceName = hostDevice.getName();
        if (hostDevice.isSetId()) {
            // in case both 'name' and 'id' is set, 'id' takes priority
            deviceName = HexUtils.hex2string(hostDevice.getId());
        }

        return performCreate(VdcActionType.AddVmHostDevices,
                new VmHostDevicesParameters(vmId, deviceName),
                new DeviceNameResolver(deviceName));
    }

    @Override
    public VmHostDeviceResource getHostDeviceSubResource(String deviceId) {
        return inject(new BackendVmHostDeviceResource(deviceId, this));
    }

    protected List<org.ovirt.engine.core.common.businessentities.HostDevice> getCollection() {
        return getBackendCollection(VdcQueryType.GetExtendedVmHostDevicesByVmId, new IdQueryParameters(vmId));
    }

    public Guid getVmId() {
        return vmId;
    }

    private class DeviceNameResolver extends EntityIdResolver<Void> {

        private final String deviceName;

        public DeviceNameResolver(String deviceName) {
            this.deviceName = deviceName;
        }

        @Override
        public org.ovirt.engine.core.common.businessentities.HostDevice lookupEntity(Void ignored) throws BackendFailureException {
            for (org.ovirt.engine.core.common.businessentities.HostDevice device : getCollection()) {
                if (device.getDeviceName().equals(deviceName)) {
                    return device;
                }
            }
            return null;
        }
    }
}
