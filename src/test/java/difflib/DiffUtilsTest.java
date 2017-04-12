package difflib;

import difflib.algorithm.DiffException;
import difflib.patch.ChangeDelta;
import difflib.patch.Chunk;
import difflib.patch.DeleteDelta;
import difflib.patch.Delta;
import difflib.patch.InsertDelta;
import difflib.patch.Patch;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.zip.ZipFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;

public class DiffUtilsTest {

    @Test
    public void testDiff_Insert() throws DiffException {
        final Patch<String> patch = DiffUtils.diff(Arrays.asList("hhh"), Arrays.
                asList("hhh", "jjj", "kkk"));
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertTrue(delta instanceof InsertDelta);
        assertEquals(new Chunk<>(1, Collections.<String>emptyList()), delta.getOriginal());
        assertEquals(new Chunk<>(1, Arrays.asList("jjj", "kkk")), delta.getRevised());
    }

    @Test
    public void testDiff_Delete() throws DiffException {
        final Patch<String> patch = DiffUtils.diff(Arrays.asList("ddd", "fff", "ggg"), Arrays.
                asList("ggg"));
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertTrue(delta instanceof DeleteDelta);
        assertEquals(new Chunk<>(0, Arrays.asList("ddd", "fff")), delta.getOriginal());
        assertEquals(new Chunk<>(0, Collections.<String>emptyList()), delta.getRevised());
    }

    @Test
    public void testDiff_Change() throws DiffException {
        final List<String> changeTest_from = Arrays.asList("aaa", "bbb", "ccc");
        final List<String> changeTest_to = Arrays.asList("aaa", "zzz", "ccc");

        final Patch<String> patch = DiffUtils.diff(changeTest_from, changeTest_to);
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertTrue(delta instanceof ChangeDelta);
        assertEquals(new Chunk<>(1, Arrays.asList("bbb")), delta.getOriginal());
        assertEquals(new Chunk<>(1, Arrays.asList("zzz")), delta.getRevised());
    }

    @Test
    public void testDiff_EmptyList() throws DiffException {
        final Patch<String> patch = DiffUtils.diff(new ArrayList<>(), new ArrayList<>());
        assertNotNull(patch);
        assertEquals(0, patch.getDeltas().size());
    }

    @Test
    public void testDiff_EmptyListWithNonEmpty() throws DiffException {
        final Patch<String> patch = DiffUtils.diff(new ArrayList<>(), Arrays.asList("aaa"));
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertTrue(delta instanceof InsertDelta);
    }

    @Test
    public void testDiffInline() throws DiffException {
        final Patch<String> patch = DiffUtils.diffInline("", "test");
        assertEquals(1, patch.getDeltas().size());
        assertTrue(patch.getDeltas().get(0) instanceof InsertDelta);
        assertEquals(0, patch.getDeltas().get(0).getOriginal().getPosition());
        assertEquals(0, patch.getDeltas().get(0).getOriginal().getLines().size());
        assertEquals("test", patch.getDeltas().get(0).getRevised().getLines().get(0));
    }

    @Test
    public void testDiffInline2() throws DiffException {
        final Patch<String> patch = DiffUtils.diffInline("es", "fest");
        assertEquals(2, patch.getDeltas().size());
        assertTrue(patch.getDeltas().get(0) instanceof InsertDelta);
        assertEquals(0, patch.getDeltas().get(0).getOriginal().getPosition());
        assertEquals(2, patch.getDeltas().get(1).getOriginal().getPosition());
        assertEquals(0, patch.getDeltas().get(0).getOriginal().getLines().size());
        assertEquals(0, patch.getDeltas().get(1).getOriginal().getLines().size());
        assertEquals("f", patch.getDeltas().get(0).getRevised().getLines().get(0));
        assertEquals("t", patch.getDeltas().get(1).getRevised().getLines().get(0));
    }

    @Test
    public void testDiffIntegerList() throws DiffException {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> revised = Arrays.asList(2, 3, 4, 6);

        final Patch<Integer> patch = DiffUtils.diff(original, revised);

        for (Delta delta : patch.getDeltas()) {
            System.out.println(delta);
        }

        assertEquals(2, patch.getDeltas().size());
        assertEquals("[DeleteDelta, position: 0, lines: [1]]", patch.getDeltas().get(0).toString());
        assertEquals("[ChangeDelta, position: 4, lines: [5] to [6]]", patch.getDeltas().get(1).toString());
    }

    @Test
    public void testDiffMissesChangeForkDnaumenkoIssue31() throws DiffException {
        List<String> original = Arrays.asList("line1", "line2", "line3");
        List<String> revised = Arrays.asList("line1", "line2-2", "line4");

        Patch<String> patch = DiffUtils.diff(original, revised);
        assertEquals(1, patch.getDeltas().size());
        assertEquals("[ChangeDelta, position: 1, lines: [line2, line3] to [line2-2, line4]]", patch.getDeltas().get(0).toString());
    }

    /**
     * To test this, the greedy meyer algorithm is not suitable.
     */
    @Test
    @Ignore
    public void testPossibleDiffHangOnLargeDatasetDnaumenkoIssue26() throws IOException, DiffException {
        ZipFile zip = new ZipFile(TestConstants.MOCK_FOLDER + "/large_dataset1.zip");
        
        Patch<String> patch = DiffUtils.diff(
                readStringListFromInputStream(zip.getInputStream(zip.getEntry("ta"))), 
                readStringListFromInputStream(zip.getInputStream(zip.getEntry("tb"))));
        
        assertEquals(1, patch.getDeltas().size());
    }
    
    private static List<String> readStringListFromInputStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())))) {
            
            return reader.lines().collect(toList());
        }
    }
}
