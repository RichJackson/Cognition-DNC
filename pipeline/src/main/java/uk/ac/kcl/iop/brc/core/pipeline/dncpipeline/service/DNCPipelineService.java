/*
        Cognition-DNC (Dynamic Name Concealer)         Developed by Ismail Kartoglu (https://github.com/iemre)
        Binary to text document converter and database pseudonymiser.

        Copyright (C) 2015 Biomedical Research Centre for Mental Health

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service;

import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.kcl.iop.brc.core.pipeline.common.exception.CanNotProcessCoordinateException;
import uk.ac.kcl.iop.brc.core.pipeline.common.helper.JsonHelper;
import uk.ac.kcl.iop.brc.core.pipeline.common.model.DNCWorkCoordinate;
import uk.ac.kcl.iop.brc.core.pipeline.common.service.DocumentConversionService;
import uk.ac.kcl.iop.brc.core.pipeline.common.service.FileTypeService;
import uk.ac.kcl.iop.brc.core.pipeline.common.utils.StringTools;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.commandline.CommandLineArgHolder;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.CoordinatesDao;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.DNCWorkUnitDao;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.PatientDao;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.Patient;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service.anonymisation.AnonymisationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DNCPipelineService {

    private static Logger logger = Logger.getLogger(DNCPipelineService.class);

    @Autowired
    private AnonymisationService anonymisationService;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private DNCWorkUnitDao dncWorkUnitDao;

    @Autowired
    private DocumentConversionService documentConversionService;

    @Value("${conversionFormat}")
    private String conversionFormat;

    @Autowired
    private FileTypeService fileTypeService;

    @Autowired
    private CoordinatesDao coordinatesDao;

    @Value("${ocrEnabled}")
    private String ocrEnabled;

    @Value("${pseudonymEnabled}")
    private String pseudonymEnabled;

    private JsonHelper<DNCWorkCoordinate> jsonHelper = new JsonHelper(DNCWorkCoordinate[].class);

    private CommandLineArgHolder commandLineArgHolder = new CommandLineArgHolder();

    private List<DNCWorkCoordinate> failedCoordinates = Collections.synchronizedList(new ArrayList<>());


    /**
     * Anonymise the DNC Work Coordinates (DWC) specified in a view/table in the source DB.
     *
     */
    public void startCreateModeWithDBView() {
        logger.info("Retrieving coordinates from DB.");

        List<DNCWorkCoordinate> dncWorkCoordinates = coordinatesDao.getCoordinates();

        dncWorkCoordinates.parallelStream().forEach(this::processSingleCoordinate);
        logger.info("Finished all.");
        dumpFailedCoordinates();
    }

    /**
     * Anonymise the DNC Work Coordinates (DWC) specified in the jSON file
     * whose path is given as argument.
     * @param filePath File path of the jSON file that contains DNC Work Coordinates.
     */
    public void startCreateModeWithFile(String filePath) {
        logger.info("Loading work units from file.");

        List<DNCWorkCoordinate> workCoordinates = jsonHelper.loadListFromFile(new File(filePath));

        workCoordinates.parallelStream().forEach(this::processSingleCoordinate);
        logger.info("Finished all.");
        dumpFailedCoordinates();
    }

    public void processCoordinates(List<DNCWorkCoordinate> coordinateQueue) {
        coordinateQueue.parallelStream().forEach(this::processSingleCoordinate);
    }

    public void processTextCoordinate(DNCWorkCoordinate coordinate) {
        try {
            logger.info("Anonymising text, coordinates: " + coordinate);
            String text = dncWorkUnitDao.getTextFromCoordinate(coordinate);
            if (pseudonymisationIsEnabled()) {
                Patient patient = patientDao.getPatient(coordinate.getPatientId());
                text = anonymisationService.pseudonymisePersonPlainText(patient, text);
            }
            saveText(coordinate, text);
        } catch (Exception ex) {
            logger.info("Could not process coordinate " + coordinate);
            failedCoordinates.add(coordinate);
            ex.printStackTrace();
        }
    }

    public void processBinaryCoordinate(DNCWorkCoordinate coordinate) {
        try {
            byte[] bytes = dncWorkUnitDao.getByteFromCoordinate(coordinate);
            String text = convertBinary(bytes);
            if (isPDFAndPossiblyOCR(bytes, text)) {
                text = applyOCRToPDF(coordinate, bytes, text);
            }
            if (pseudonymisationIsEnabled()) {
                logger.info("Pseudonymising binary, coordinates: " + coordinate);
                Patient patient = patientDao.getPatient(coordinate.getPatientId());
                text = pseudonymisePersonText(patient, text);
            }
            if (StringTools.noContentInHtml(text)) {
                logger.warn("Not saving empty document at coordinate: " + coordinate);
            } else {
                saveText(coordinate, text);
            }
        } catch (Exception ex) {
            logger.error("Could not process coordinate " + coordinate );
            failedCoordinates.add(coordinate);
            ex.printStackTrace();
        }
    }

    private String applyOCRToPDF(DNCWorkCoordinate coordinate, byte[] bytes, String text) throws CanNotProcessCoordinateException {
        String metaData = StringTools.getMetaDataFromHTML(text);
        text = documentConversionService.tryOCRByConvertingToTiff(coordinate, bytes);
        text = StringTools.addMetaDataToHtml(text, metaData);
        return text;
    }

    private boolean isPDFAndPossiblyOCR(byte[] bytes, String text) {
        return StringTools.noContentInHtml(text) && fileTypeService.isPDF(bytes);
    }

    private boolean pseudonymisationIsEnabled() {
        if (! commandLineArgHolder.isNoPseudonym()) {
            return true;
        }
        return "1".equals(pseudonymEnabled) || "true".equalsIgnoreCase(pseudonymEnabled);
    }

    private String pseudonymisePersonText(Patient patient, String text) {
        if (conversionPreferenceIsHTML()) {
            text = anonymisationService.pseudonymisePersonHTML(patient, text);
        } else {
            text = anonymisationService.pseudonymisePersonPlainText(patient, text);
        }
        return text;
    }

    private String convertBinary(byte[] bytes) {
        if (conversionPreferenceIsHTML()) {
            return documentConversionService.convertToXHTML(bytes);
        }
        return documentConversionService.convertToText(bytes);
    }

    private boolean conversionPreferenceIsHTML() {
        return conversionFormat.equalsIgnoreCase("html") || conversionFormat.equalsIgnoreCase("xhtml");
    }

    private void saveText(DNCWorkCoordinate coordinate, String text) {
        dncWorkUnitDao.saveConvertedText(coordinate, text);
    }

    public void processFile(String absoluteFilePath) {
        File file = new File(absoluteFilePath);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = IOUtils.toByteArray(fileInputStream);
            String text = convertBinary(bytes);
            DNCWorkCoordinate coordinate = DNCWorkCoordinate.createEmptyCoordinate();
            coordinate.setSourceTable(absoluteFilePath);
            if (isPDFAndPossiblyOCR(bytes, text)) {
                text = applyOCRToPDF(coordinate, bytes, text);
            }
            saveText(coordinate, text);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void setConversionFormat(String conversionFormat) {
        this.conversionFormat = conversionFormat;
    }

    private void processSingleCoordinate(DNCWorkCoordinate coordinate) {
        logger.info("Processing coordinate " + coordinate);
        if (coordinate.isBinary()) {
            processBinaryCoordinate(coordinate);
        } else {
            processTextCoordinate(coordinate);
        }
    }

    public void dumpFailedCoordinates() {
        if (CollectionUtils.isEmpty(failedCoordinates)) {
            return;
        }
        Gson gson = new Gson();
        String failedJson = gson.toJson(failedCoordinates);
        PrintWriter writer;
        try {
            writer = new PrintWriter("failedCoordinates.json", "UTF-8");
            writer.println(failedJson);
            writer.close();
            logger.info("Dumped all failed coordinates to failedCoordinates.json. You can process them by --createMode --file=failedCoordinates.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CommandLineArgHolder getCommandLineArgHolder() {
        return commandLineArgHolder;
    }

    public void setOcrEnabled(String ocrEnabled) {
        this.ocrEnabled = ocrEnabled;
    }
}
