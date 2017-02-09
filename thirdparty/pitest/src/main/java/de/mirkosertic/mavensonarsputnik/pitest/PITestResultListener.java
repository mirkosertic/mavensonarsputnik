package de.mirkosertic.mavensonarsputnik.pitest;

import org.pitest.coverage.TestInfo;
import org.pitest.functional.Option;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.util.ResultOutputStrategy;
import org.pitest.util.StringUtil;
import org.pitest.util.Unchecked;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

public class PITestResultListener implements MutationResultListener {

    enum Tag {
        mutation,
        sourceFile,
        mutatedClass,
        mutatedMethod,
        methodDescription,
        lineNumber,
        mutator,
        index,
        killingTest,
        testInfo,
        testInfos,
        description;
    }

    private final Writer out;

    public PITestResultListener(ResultOutputStrategy outputStrategy) {
        this(outputStrategy.createWriterForFile("extended-mutations.xml"));
    }

    public PITestResultListener(Writer aOut) {
        out = aOut;
    }

    private void writeResult(ClassMutationResults metaData) {
        Iterator var2 = metaData.getMutations().iterator();

        while (var2.hasNext()) {
            MutationResult mutation = (MutationResult) var2.next();
            writeMutationResultXML(mutation);
        }

    }

    private void writeMutationResultXML(MutationResult result) {
        write(makeNode(makeMutationNode(result), makeMutationAttributes(result), Tag.mutation) + "\n");
    }

    private String makeMutationAttributes(MutationResult result) {
        return "detected=\'" + result.getStatus().isDetected() + "\' status=\'" + result.getStatus() + "\'";
    }

    private String makeMutationNode(MutationResult mutation) {
        MutationDetails details = mutation.getDetails();
        return makeNode(clean(details.getFilename()), Tag.sourceFile)
                + makeNode(clean(details.getClassName().asJavaName()), Tag.mutatedClass)
                + makeNode(clean(details.getMethod().name()), Tag.mutatedMethod)
                + makeNode(clean(details.getId().getLocation().getMethodDesc()), Tag.methodDescription)
                + makeNode("" + details.getLineNumber(), Tag.lineNumber)
                + makeNode(clean(details.getMutator()), Tag.mutator) + makeNode("" + details.getFirstIndex(), Tag.index)
                + makeNode(createKillingTestDesc(mutation.getKillingTest()), Tag.killingTest)
                + makeTestInfoNode(details)
                + makeNode(clean(details.getDescription()), Tag.description);
    }

    private String makeTestInfoNode(MutationDetails aDetails) {
        List<TestInfo> tests = aDetails.getTestsInOrder();

        if (tests == null || tests.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (TestInfo info : tests) {
            result.append(makeNode(info.getName(), Tag.testInfo));
        }
        return makeNode(result.toString(), Tag.testInfos);
    }

    private String clean(String value) {
        return StringUtil.escapeBasicHtmlChars(value);
    }

    private String makeNode(String value, String attributes, Tag tag) {
        return value != null ? "<" + tag + " " + attributes + ">" + value + "</" + tag + ">" : "<" + tag + attributes + "/>";
    }

    private String makeNode(String value, Tag tag) {
        return value != null ? "<" + tag + ">" + value + "</" + tag + ">" : "<" + tag + "/>";
    }

    private String createKillingTestDesc(Option<String> killingTest) {
        return killingTest.hasSome() ? clean((String) killingTest.value()) : null;
    }

    private void write(String value) {
        try {
            out.write(value);
        } catch (IOException var3) {
            throw Unchecked.translateCheckedException(var3);
        }
    }

    public void runStart() {
        write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        write("<mutations>\n");
    }

    public void handleMutationResult(ClassMutationResults metaData) {
        writeResult(metaData);
    }

    public void runEnd() {
        try {
            write("</mutations>\n");
            out.close();
        } catch (IOException var2) {
            throw Unchecked.translateCheckedException(var2);
        }
    }
}