/*
        Copyright (c) 2015 King's College London

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service.anonymisation;


import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.Patient;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class NHSNumberPseudonymiser extends Pseudonymiser {

    @Override
    public String getJsonRuleFilePath() {
        return "anonymisation/nhsIdRules.json";
    }

    @Override
    public boolean canIgnore(Patient patient) {
        return StringUtils.isBlank(patient.getNHSNumber());
    }

}
