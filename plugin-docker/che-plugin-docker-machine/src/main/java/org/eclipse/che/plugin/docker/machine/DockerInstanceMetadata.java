/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.docker.machine;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.ServerImpl;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;
import org.eclipse.che.api.machine.shared.Server;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.client.json.PortBinding;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker implemantation of {@link InstanceMetadata}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerInstanceMetadata implements InstanceMetadata {

    private final ContainerInfo info;
    private final String        hostAddress;

    @Inject
    public DockerInstanceMetadata(DockerConnector docker,
                                  @Named("api.endpoint") String apiEndpoint,
                                  @Assisted String container) throws MachineException {
        try {
            info = docker.inspectContainer(container);

            hostAddress = new URI(apiEndpoint).getHost();
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        } catch (URISyntaxException e) {
            throw new MachineException("Configuration error. Property 'api.endpoint is not valid URI. '" + e.getLocalizedMessage());
        }

    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> md = new LinkedHashMap<>();
        md.put("id", info.getId());
        md.put("created", info.getCreated());
        md.put("image", info.getImage());
        md.put("path", info.getPath());
        md.put("appArmorProfile", info.getAppArmorProfile());
        md.put("driver", info.getDriver());
        md.put("execDriver", info.getExecDriver());
        md.put("hostnamePath", info.getHostnamePath());
        md.put("hostsPath", info.getHostsPath());
        md.put("mountLabel", info.getMountLabel());
        md.put("name", info.getName());
        md.put("processLabel", info.getProcessLabel());
        md.put("volumesRW", String.valueOf(info.getVolumesRW()));
        md.put("resolvConfPath", info.getResolvConfPath());
        md.put("sysInitPath", info.getSysInitPath());
        md.put("args", Arrays.toString(info.getArgs()));
        md.put("volumes", String.valueOf(info.getVolumes()));
        md.put("config.domainName", info.getConfig().getDomainName());
        md.put("config.hostname", info.getConfig().getHostname());
        md.put("config.image", info.getConfig().getImage());
        md.put("config.user", info.getConfig().getUser());
        md.put("config.workingDir", info.getConfig().getWorkingDir());
        md.put("config.cmd", Arrays.toString(info.getConfig().getCmd()));
        md.put("config.volumes", String.valueOf(info.getConfig().getVolumes()));
        md.put("config.cpuset", info.getConfig().getCpuset());
        md.put("config.entrypoint", info.getConfig().getEntrypoint());
        md.put("config.exposedPorts", String.valueOf(info.getConfig().getExposedPorts()));
        md.put("config.macAddress", info.getConfig().getMacAddress());
        md.put("config.securityOpts", Arrays.toString(info.getConfig().getSecurityOpts()));
        md.put("config.cpuShares", Integer.toString(info.getConfig().getCpuShares()));
        md.put("config.env", Arrays.toString(info.getConfig().getEnv()));
        md.put("config.memory", Long.toString(info.getConfig().getMemory()));
        md.put("config.memorySwap", Long.toString(info.getConfig().getMemorySwap()));
        md.put("config.attachStderr", Boolean.toString(info.getConfig().isAttachStderr()));
        md.put("config.attachStdin", Boolean.toString(info.getConfig().isAttachStdin()));
        md.put("config.attachStdout", Boolean.toString(info.getConfig().isAttachStdout()));
        md.put("config.networkDisabled", Boolean.toString(info.getConfig().isNetworkDisabled()));
        md.put("config.openStdin", Boolean.toString(info.getConfig().isOpenStdin()));
        md.put("config.stdinOnce", Boolean.toString(info.getConfig().isStdinOnce()));
        md.put("config.tty", Boolean.toString(info.getConfig().isTty()));
        md.put("state.startedAt", info.getState().getStartedAt());
        md.put("state.exitCode", Integer.toString(info.getState().getExitCode()));
        md.put("state.pid", Integer.toString(info.getState().getPid()));
        md.put("state.ghost", Boolean.toString(info.getState().isGhost()));
        md.put("state.running", Boolean.toString(info.getState().isRunning()));
        md.put("state.finishedAt", info.getState().getFinishedAt());
        md.put("state.paused", Boolean.toString(info.getState().isPaused()));
        md.put("state.restarting", Boolean.toString(info.getState().isRestarting()));
        md.put("network.bridge", info.getNetworkSettings().getBridge());
        md.put("network.gateway", info.getNetworkSettings().getGateway());
        md.put("network.ipAddress", info.getNetworkSettings().getIpAddress());
        md.put("network.ipPrefixLen", Integer.toString(info.getNetworkSettings().getIpPrefixLen()));
        md.put("network.portMappings", Arrays.toString(info.getNetworkSettings().getPortMapping()));
        md.put("network.macAddress", info.getNetworkSettings().getMacAddress());
        md.put("network.ports", String.valueOf(info.getNetworkSettings().getPorts()));
        return md;
    }

    @Override
    public String toJson() {
        return info.toString();
    }

    @Override
    public Map<String, Server> getServers() {
        final HashMap<String, Server> exposedPorts = new HashMap<>();
        for (Map.Entry<String, List<PortBinding>> portEntry : info.getNetworkSettings().getPorts().entrySet()) {
            // in form 1234/tcp or 1234
            final String portProtocol = portEntry.getKey();
            // we are assigning ports automatically, so have 1 to 1 binding (at least per protocol)
            final PortBinding portBinding = portEntry.getValue().get(0);
            if (portProtocol.endsWith("/udp")) {
                exposedPorts.put(portProtocol, new ServerImpl(hostAddress + ":" + portBinding.getHostPort()));
            } else {
                // cut off /tcp if it presents
                exposedPorts.put(portProtocol.split("/", 2)[0], new ServerImpl(hostAddress + ":" + portBinding.getHostPort()));
            }
        }
        return exposedPorts;
    }
}
