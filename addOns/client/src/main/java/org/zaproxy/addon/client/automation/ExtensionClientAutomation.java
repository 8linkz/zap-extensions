/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2024 The ZAP Development Team
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
package org.zaproxy.addon.client.automation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import org.parosproxy.paros.CommandLine;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.zaproxy.addon.automation.ExtensionAutomation;
import org.zaproxy.addon.client.ExtensionClientIntegration;

public class ExtensionClientAutomation extends ExtensionAdaptor {

    public static final String NAME = "ExtensionClientAutomation";

    private static final String RESOURCES_DIR = "/org/zaproxy/addon/client/resources/";

    private static final List<Class<? extends Extension>> DEPENDENCIES =
            List.of(ExtensionClientIntegration.class, ExtensionAutomation.class);

    private ClientSpiderJob job;

    public ExtensionClientAutomation() {
        super(NAME);
    }

    @Override
    public boolean supportsDb(String type) {
        return true;
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);
        ExtensionAutomation extAuto =
                Control.getSingleton().getExtensionLoader().getExtension(ExtensionAutomation.class);
        job = new ClientSpiderJob();
        extAuto.registerAutomationJob(job);
    }

    @Override
    public boolean canUnload() {
        return true;
    }

    @Override
    public void unload() {
        ExtensionAutomation extAuto =
                Control.getSingleton().getExtensionLoader().getExtension(ExtensionAutomation.class);

        extAuto.unregisterAutomationJob(job);
    }

    @Override
    public List<Class<? extends Extension>> getDependencies() {
        return DEPENDENCIES;
    }

    public static String getResourceAsString(String name) {
        try (InputStream in =
                ExtensionClientIntegration.class.getResourceAsStream(RESOURCES_DIR + name)) {
            return new BufferedReader(new InputStreamReader(in))
                            .lines()
                            .collect(Collectors.joining("\n"))
                    + "\n";
        } catch (Exception e) {
            CommandLine.error(
                    Constant.messages.getString(
                            "client.automation.error.nofile", RESOURCES_DIR + name));
        }
        return "";
    }

    @Override
    public String getDescription() {
        return Constant.messages.getString("client.automation.desc");
    }

    @Override
    public String getUIName() {
        return Constant.messages.getString("client.automation.name");
    }
}
