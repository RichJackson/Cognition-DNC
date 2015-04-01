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

package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.typehandlers;

import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.DbColumnTypes;
import org.springframework.stereotype.Component;

@Component
public abstract class TypeHandler {

    public abstract boolean canHandle(String databaseName);

    public abstract DbColumnTypes getDbColumnTypeForString(String typeStr);

}
