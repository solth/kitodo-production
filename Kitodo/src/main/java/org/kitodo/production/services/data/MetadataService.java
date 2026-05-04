/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.services.data;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.kitodo.api.Metadata;
import org.kitodo.api.dataeditor.rulesetmanagement.RulesetManagementInterface;
import org.kitodo.api.dataeditor.rulesetmanagement.StructuralElementViewInterface;
import org.kitodo.api.dataformat.LogicalDivision;
import org.kitodo.api.dataformat.Workpiece;
import org.kitodo.constants.StringConstants;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.User;
import org.kitodo.exceptions.FileStructureValidationException;
import org.kitodo.exceptions.InvalidMetadataValueException;
import org.kitodo.exceptions.NoRecordFoundException;
import org.kitodo.exceptions.NoSuchMetadataFieldException;
import org.kitodo.exceptions.ProcessGenerationException;
import org.kitodo.exceptions.UnsupportedFormatException;
import org.kitodo.production.forms.createprocess.ProcessDetail;
import org.kitodo.production.forms.createprocess.ProcessFieldedMetadata;
import org.kitodo.production.helper.MetadataComparison;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.dataeditor.DataEditorService;
import org.primefaces.model.TreeNode;
import org.xml.sax.SAXException;

public class MetadataService {

    /**
     * Extracts and returns metadata from the descendants of the given tree node.
     *
     * @param treeNode
     *            the root node whose children are to be processed to extract metadata
     * @return a HashSet containing metadata entries collected from the descendants of the provided tree node
     * @throws InvalidMetadataValueException
     *             if metadata values encountered during processing are invalid
     */
    public static HashSet<Metadata> getMetadata(TreeNode<Object> treeNode) throws InvalidMetadataValueException {
        HashSet<Metadata> processDetails = new HashSet<>();
        for (TreeNode<Object> child : treeNode.getChildren()) {
            processDetails.addAll(((ProcessDetail) child.getData()).getMetadata(false));
        }
        return processDetails;
    }

    /**
     * Updates the metadata of a given process by re-importing, validating, and adjusting its catalog metadata.
     * The method retrieves the metadata file URI associated with the process, loads the corresponding workpiece,
     * and re-imports the metadata from the import source associated with the process.
     * The existing metadata is compared with the new values and updated according to ruleset configurations.
     * A backup of the process is created before saving the updated metadata.
     *
     * @param process
     *        the Process whose metadata needs to be updated
     *
     * @throws IOException
     *         if an I/O error occurs while accessing or saving metadata files
     * @throws FileStructureValidationException
     *         if XML validation of current or updated process metadata fails
     * @throws SAXException
     *         if an error occurs during XML parsing
     * @throws InvalidMetadataValueException
     *         if invalid metadata values are encountered during the update process
     * @throws UnsupportedFormatException
     *         if the format of the imported metadata is unsupported
     * @throws XPathExpressionException
     *         if there are issues evaluating XPath expressions during the metadata processing
     * @throws NoRecordFoundException
     *         if no corresponding record is found in the import source during the metadata update process
     * @throws ProcessGenerationException
     *         if an error occurs during the generation of the process metadata
     * @throws ParserConfigurationException
     *         if a configuration error occurs during the XML parser initialization
     * @throws URISyntaxException
     *         if the URI of the metadata file cannot be constructed or is invalid
     * @throws TransformerException
     *         if an error occurs during metadata transformation or saving
     * @throws NoSuchMetadataFieldException
     *         if a required metadata field is not found during processing
     */
    public static void updateMetadata(Process process) throws IOException, FileStructureValidationException,
            SAXException, InvalidMetadataValueException, UnsupportedFormatException, XPathExpressionException,
            NoRecordFoundException, ProcessGenerationException, ParserConfigurationException, URISyntaxException,
            TransformerException, NoSuchMetadataFieldException {
        boolean validate = Objects.nonNull(process) && Objects.nonNull(process.getImportConfiguration())
                && process.getImportConfiguration().getValidateExternalData();
        URI processUri = ServiceManager.getProcessService().getMetadataFileUri(process);
        Workpiece workpiece = ServiceManager.getMetsService().loadWorkpiece(processUri);
        LogicalDivision logicalRoot = workpiece.getLogicalStructure();
        String structureType = logicalRoot.getType();
        User user = ServiceManager.getUserService().getCurrentUser();
        String metadataLanguage = user.getMetadataLanguage();
        List<Locale.LanguageRange> priorityList = Locale.LanguageRange.parse(metadataLanguage.isEmpty() ? "en" : metadataLanguage);
        RulesetManagementInterface managementInterface = ServiceManager.getRulesetService().openRuleset(process.getRuleset());
        StructuralElementViewInterface divisionView = managementInterface.getStructuralElementView(structureType, StringConstants.EDIT,
                priorityList);
        ProcessFieldedMetadata fieldedMetadata = new ProcessFieldedMetadata(logicalRoot, divisionView, managementInterface);
        HashSet<Metadata> existingMetadata = getMetadata(fieldedMetadata.getTreeNode());
        List<MetadataComparison> metadataComparisons = DataEditorService.reimportCatalogMetadata(process, workpiece, existingMetadata,
                priorityList, structureType, validate);
        DataEditorService.updateMetadataWithNewValues(logicalRoot, metadataComparisons);
        ServiceManager.getFileService().createBackupFile(process);
        ServiceManager.getMetsService().saveWorkpiece(workpiece, processUri);
    }
}
