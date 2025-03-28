/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
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
package org.zaproxy.addon.pscan.automation.jobs;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.parosproxy.paros.CommandLine;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.zaproxy.addon.automation.AutomationData;
import org.zaproxy.addon.automation.AutomationEnvironment;
import org.zaproxy.addon.automation.AutomationJob;
import org.zaproxy.addon.automation.AutomationProgress;
import org.zaproxy.addon.automation.JobResultData;
import org.zaproxy.addon.automation.jobs.JobData;
import org.zaproxy.addon.automation.jobs.JobUtils;
import org.zaproxy.addon.pscan.ExtensionPassiveScan2;
import org.zaproxy.addon.pscan.automation.internal.PassiveScanWaitJobDialog;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;

public class PassiveScanWaitJob extends AutomationJob {

    public static final String JOB_NAME = "passiveScan-wait";

    private static final String PARAM_MAX_DURATION = "maxDuration";

    private final ExtensionPassiveScan2 pscan;

    private Data data;
    private Parameters parameters = new Parameters();

    public PassiveScanWaitJob() {
        this.pscan =
                Control.getSingleton()
                        .getExtensionLoader()
                        .getExtension(ExtensionPassiveScan2.class);
        this.data = new Data(this, parameters);
    }

    @Override
    public boolean supportsAlertTests() {
        return true;
    }

    @Override
    public String getKeyAlertTestsResultData() {
        return PassiveScanJobResultData.KEY;
    }

    @Override
    public void runJob(AutomationEnvironment env, AutomationProgress progress) {
        long endTime = Long.MAX_VALUE;
        Integer maxDuration = this.parameters.getMaxDuration();
        if (maxDuration != null && maxDuration > 0) {
            endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(maxDuration);
        }

        while (pscan.getRecordsToScan() > 0) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        progress.addJobResultData(this.getJobResultData());
    }

    @Override
    @SuppressWarnings("removal")
    public List<JobResultData> getJobResultData() {
        List<JobResultData> list = new ArrayList<>();
        List<PluginPassiveScanner> scanRules = pscan.getPassiveScannersManager().getScanRules();
        list.add(new PassiveScanJobResultData(this.getName(), scanRules));
        // XXX Provided for compatibility with older add-ons.
        list.add(
                new org.zaproxy.addon.automation.jobs.PassiveScanJobResultData(
                        getName(), scanRules));
        return list;
    }

    @Override
    public void verifyParameters(AutomationProgress progress) {
        Map<?, ?> jobData = this.getJobData();
        if (jobData == null) {
            return;
        }
        JobUtils.applyParamsToObject(
                (Map<?, ?>) jobData.get("parameters"),
                this.parameters,
                this.getName(),
                null,
                progress);
    }

    @Override
    public void applyParameters(AutomationProgress progress) {
        // Nothing to do
    }

    @Override
    public Map<String, String> getCustomConfigParameters() {
        Map<String, String> map = super.getCustomConfigParameters();
        map.put(PARAM_MAX_DURATION, "0");
        return map;
    }

    @Override
    public Data getData() {
        return this.data;
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void showDialog() {
        new PassiveScanWaitJobDialog(this).setVisible(true);
    }

    @Override
    public String getTemplateDataMin() {
        return getResourceAsString(getType() + "-min.yaml");
    }

    @Override
    public String getTemplateDataMax() {
        return getResourceAsString(getType() + "-max.yaml");
    }

    private static String getResourceAsString(String fileName) {
        try (InputStream in = PassiveScanConfigJob.class.getResourceAsStream(fileName)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            CommandLine.error(
                    Constant.messages.getString("pscan.automation.error.nofile", fileName));
        }
        return "";
    }

    @Override
    public String getSummary() {
        return Constant.messages.getString(
                "pscan.automation.dialog.pscanwait.summary", this.getParameters().getMaxDuration());
    }

    @Override
    public String getType() {
        return JOB_NAME;
    }

    @Override
    public Order getOrder() {
        return Order.AFTER_EXPLORE;
    }

    @Override
    public Object getParamMethodObject() {
        return null;
    }

    @Override
    public String getParamMethodName() {
        return null;
    }

    @Getter
    public static class Data extends JobData {
        private final Parameters parameters;

        public Data(AutomationJob job, Parameters parameters) {
            super(job);
            this.parameters = parameters;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameters extends AutomationData {
        private Integer maxDuration = 0;
    }
}
