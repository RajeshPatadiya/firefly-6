package test.utils.lang;

import com.firefly.utils.lang.URIUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class URIUtilsCanonicalPathTest {
    @Parameterized.Parameters(name = "{0}")
    public static List<String[]> data() {
        String[][] canonical =
                {
                        // Basic examples (no changes expected)
                        {"/hello.html", "/hello.html"},
                        {"/css/main.css", "/css/main.css"},
                        {"/", "/"},
                        {"", ""},
                        {"/aaa/bbb/", "/aaa/bbb/"},
                        {"/aaa/bbb", "/aaa/bbb"},
                        {"aaa/bbb", "aaa/bbb"},
                        {"aaa/", "aaa/"},
                        {"aaa", "aaa"},
                        {"a", "a"},
                        {"a/", "a/"},

                        // Extra slashes
                        {"/aaa//bbb/", "/aaa//bbb/"},
                        {"/aaa//bbb", "/aaa//bbb"},
                        {"/aaa///bbb/", "/aaa///bbb/"},

                        // Path traversal with current references "./"
                        {"/aaa/./bbb/", "/aaa/bbb/"},
                        {"/aaa/./bbb", "/aaa/bbb"},
                        {"./bbb/", "bbb/"},
                        {"./aaa/../bbb/", "bbb/"},
                        {"/foo/.", "/foo/"},
                        {"./", ""},
                        {".", ""},
                        {".//", "/"},
                        {".///", "//"},
                        {"/.", "/"},
                        {"//.", "//"},
                        {"///.", "///"},

                        // Path traversal directory (but not past root)
                        {"/aaa/../bbb/", "/bbb/"},
                        {"/aaa/../bbb", "/bbb"},
                        {"/aaa..bbb/", "/aaa..bbb/"},
                        {"/aaa..bbb", "/aaa..bbb"},
                        {"/aaa/..bbb/", "/aaa/..bbb/"},
                        {"/aaa/..bbb", "/aaa/..bbb"},
                        {"/aaa/./../bbb/", "/bbb/"},
                        {"/aaa/./../bbb", "/bbb"},
                        {"/aaa/bbb/ccc/../../ddd/", "/aaa/ddd/"},
                        {"/aaa/bbb/ccc/../../ddd", "/aaa/ddd"},
                        {"/foo/../bar//", "/bar//"},
                        {"/ctx/../bar/../ctx/all/index.txt", "/ctx/all/index.txt"},
                        {"/down/.././index.html", "/index.html"},

                        // Path traversal up past root
                        {"..", null},
                        {"./..", null},
                        {"aaa/../..", null},
                        {"/foo/bar/../../..", null},
                        {"/../foo", null},
                        {"a/.", "a/"},
                        {"a/..", ""},
                        {"a/../..", null},
                        {"/foo/../../bar", null},

                        // Query parameter specifics
                        {"/ctx/dir?/../index.html", "/ctx/index.html"},
                        {"/get-files?file=/etc/passwd", "/get-files?file=/etc/passwd"},
                        {"/get-files?file=../../../../../passwd", null},

                        // Known windows shell quirks
                        {"file.txt  ", "file.txt  "}, // with spaces
                        {"file.txt...", "file.txt..."}, // extra dots ignored by windows
                        // BREAKS Jenkins: {"file.txt\u0000", "file.txt\u0000"}, // null terminated is ignored by windows
                        {"file.txt\r", "file.txt\r"}, // CR terminated is ignored by windows
                        {"file.txt\n", "file.txt\n"}, // LF terminated is ignored by windows
                        {"file.txt\"\"\"\"", "file.txt\"\"\"\""}, // extra quotes ignored by windows
                        {"file.txt<<<>>><", "file.txt<<<>>><"}, // angle brackets at end of path ignored by windows
                        {"././././././file.txt", "file.txt"},

                        // Oddball requests that look like path traversal, but are not
                        {"/....", "/...."},
                        {"/..../ctx/..../blah/logo.jpg", "/..../ctx/..../blah/logo.jpg"},

                        // paths with encoded segments should remain encoded
                        // canonicalPath() is not responsible for decoding characters
                        {"%2e%2e/", "%2e%2e/"},
                };
        return Arrays.asList(canonical);
    }

    @Parameterized.Parameter(0)
    public String input;

    @Parameterized.Parameter(1)
    public String expectedResult;

    @Test
    public void testCanonicalPath() {
        assertThat("Canonical", URIUtils.canonicalPath(input), is(expectedResult));
    }

}
