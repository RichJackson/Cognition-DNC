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

package uk.ac.kcl.iop.brc.core.pipeline.common.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class DocumentConversionService {

    public String convertToText(byte[] data) {
        return convertWithHandler(data, new BodyContentHandler());
    }

    public String convertToXHTML(byte[] data) {
        return convertWithHandler(data, new ToXMLContentHandler());
    }

    private String convertWithHandler(byte[] data, DefaultHandler handler) {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (InputStream stream = new ByteArrayInputStream(data)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
