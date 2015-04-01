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
package uk.ac.kcl.iop.brc.core.pipeline.common.ml;

import opennlp.tools.doccat.DocumentCategorizer;

import java.util.HashMap;
import java.util.Map;

public class DocumentClassifier {

    public Map<String, Double> getTopicScores(DocumentCategorizer documentCategorizer, String text) {
        Map<String, Double> scores = new HashMap<>();
        double[] categoryScores = documentCategorizer.categorize(text);
        int numberOfCategories = documentCategorizer.getNumberOfCategories();

        for (int i = 0; i < numberOfCategories; i++) {
            String category = documentCategorizer.getCategory(i);
            scores.put(category, categoryScores[documentCategorizer.getIndex(category)]);
        }

        return scores;
    }

}
