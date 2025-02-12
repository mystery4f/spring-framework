/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Miscellaneous {@link String} utility methods.
 *
 * <p>Mainly for internal use within the framework; consider
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache's Commons Lang</a>
 * for a more comprehensive suite of {@code String} utilities.
 *
 * <p>This class delivers some simple functionality that should really be
 * provided by the core Java {@link String} and {@link StringBuilder}
 * classes. It also provides easy-to-use methods to convert between
 * delimited strings, such as CSV strings, and collections and arrays.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 16 April 2001
 */
public abstract class StringUtils {

	private static final String[] EMPTY_STRING_ARRAY = {};

	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	private static final char EXTENSION_SEPARATOR = '.';


	//---------------------------------------------------------------------
	// General convenience methods for working with Strings
	//---------------------------------------------------------------------

	/**
	 * 检查给定的对象（可能是{@code String}）是否为空。
	 * 这是一种快捷方式，用于{@code !hasLength(String)}。
	 * <p>此方法接受任何对象作为参数，将其与{@code null}和空字符串进行比较。
	 * 因此，对于非空非字符串对象，此方法将永远不会返回{@code true}。
	 * <p>Object签名对于处理属性的通用代码非常有用，因为属性可能
	 * 例如，也是原始值对象。
	 * <p><b>注意：如果对象被类型化为{@code String}，则优先考虑
	 * {@link #hasLength(String)}或{@link #hasText(String)}。</b>
	 *
	 * @param str 候选对象（可能是{@code String}）
	 * @since 3.2.1
	 * @deprecated 从5.3开始，转为支持{@link #hasLength(String)}和
	 * {@link #hasText(String)}（或{@link ObjectUtils#isEmpty(Object)})
	 */
	@Deprecated
	public static boolean isEmpty(@Nullable Object str) {
		return (str == null || "".equals(str));
	}

	/**
	 * Check whether the given {@code CharSequence} contains actual <em>text</em>.
	 * <p>More specifically, this method returns {@code true} if the
	 * {@code CharSequence} is not {@code null}, its length is greater than
	 * 0, and it contains at least one non-whitespace character.
	 * <p><pre class="code">
	 * StringUtils.hasText(null) = false
	 * StringUtils.hasText("") = false
	 * StringUtils.hasText(" ") = false
	 * StringUtils.hasText("12345") = true
	 * StringUtils.hasText(" 12345 ") = true
	 * </pre>
	 *
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not {@code null},
	 * its length is greater than 0, and it does not contain whitespace only
	 * @see #hasText(String)
	 * @see #hasLength(CharSequence)
	 * @see Character#isWhitespace
	 */
	public static boolean hasText(@Nullable CharSequence str) {
		return (str != null && str.length() > 0 && containsText(str));
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given {@code String} contains any whitespace characters.
	 *
	 * @param str the {@code String} to check (may be {@code null})
	 * @return {@code true} if the {@code String} is not empty and
	 * contains at least 1 whitespace character
	 * @see #containsWhitespace(CharSequence)
	 */
	public static boolean containsWhitespace(@Nullable String str) {
		return containsWhitespace((CharSequence) str);
	}

	/**
	 * Check whether the given {@code CharSequence} contains any whitespace characters.
	 *
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not empty and
	 * contains at least 1 whitespace character
	 * @see Character#isWhitespace
	 */
	public static boolean containsWhitespace(@Nullable CharSequence str) {
		if (!hasLength(str)) {
			return false;
		}

		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check that the given {@code CharSequence} is neither {@code null} nor
	 * of length 0.
	 * <p>Note: this method returns {@code true} for a {@code CharSequence}
	 * that purely consists of whitespace.
	 * <p><pre class="code">
	 * StringUtils.hasLength(null) = false
	 * StringUtils.hasLength("") = false
	 * StringUtils.hasLength(" ") = true
	 * StringUtils.hasLength("Hello") = true
	 * </pre>
	 *
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not {@code null} and has length
	 * @see #hasLength(String)
	 * @see #hasText(CharSequence)
	 */
	public static boolean hasLength(@Nullable CharSequence str) {
		return (str != null && str.length() > 0);
	}

	/**
	 * Trim leading and trailing whitespace from the given {@code String}.
	 *
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimWhitespace(String str) {
		// 如果字符串为空，则直接返回原字符串
		if (!hasLength(str)) {
			return str;
		}

		// 初始化开始索引和结束索引
		int beginIndex = 0;
		int endIndex = str.length() - 1;

		// 从开始索引开始，找到第一个非空格字符
		while (beginIndex <= endIndex && Character.isWhitespace(str.charAt(beginIndex))) {
			beginIndex++;
		}

		// 从结束索引开始，找到第一个非空格字符
		while (endIndex > beginIndex && Character.isWhitespace(str.charAt(endIndex))) {
			endIndex--;
		}

		// 返回截取后的字符串
		return str.substring(beginIndex, endIndex + 1);
	}

	/**
	 * Check that the given {@code String} is neither {@code null} nor of length 0.
	 * <p>Note: this method returns {@code true} for a {@code String} that
	 * purely consists of whitespace.
	 *
	 * @param str the {@code String} to check (may be {@code null})
	 * @return {@code true} if the {@code String} is not {@code null} and has length
	 * @see #hasLength(CharSequence)
	 * @see #hasText(String)
	 */
	public static boolean hasLength(@Nullable String str) {
		return (str != null && !str.isEmpty());
	}

	/**
	 * Trim <i>all</i> whitespace from the given {@code String}:
	 * leading, trailing, and in between characters.
	 *
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimAllWhitespace(String str) {
		// 如果字符串为空，则直接返回原字符串
		if (!hasLength(str)) {
			return str;
		}

		// 获取字符串长度
		int len = str.length();
		// 创建一个新的字符串构建器，用于存储去除空格后的字符串
		StringBuilder sb = new StringBuilder(str.length());
		// 遍历字符串中的每一个字符
		for (int i = 0; i < len; i++) {
			// 获取字符
			char c = str.charAt(i);
			// 如果字符不是空格，则将其添加到字符串构建器中
			if (!Character.isWhitespace(c)) {
				sb.append(c);
			}
		}
		// 返回去除空格后的字符串
		return sb.toString();
	}

	/**
	 * Trim trailing whitespace from the given {@code String}.
	 *
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimTrailingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int endIdx = str.length() - 1;
		while (endIdx >= 0 && Character.isWhitespace(str.charAt(endIdx))) {
			endIdx--;
		}
		return str.substring(0, endIdx + 1);
	}

	/**
	 * Trim all occurrences of the supplied trailing character from the given {@code String}.
	 *
	 * @param str               the {@code String} to check
	 * @param trailingCharacter the trailing character to be trimmed
	 * @return the trimmed {@code String}
	 */
	public static String trimTrailingCharacter(String str, char trailingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		int endIdx = str.length() - 1;
		while (endIdx >= 0 && trailingCharacter == str.charAt(endIdx)) {
			endIdx--;
		}
		return str.substring(0, endIdx + 1);
	}

	/**
	 * Test if the given {@code String} matches the given single character.
	 *
	 * @param str             the {@code String} to check
	 * @param singleCharacter the character to compare to
	 * @since 5.2.9
	 */
	public static boolean matchesCharacter(@Nullable String str, char singleCharacter) {
		return (str != null && str.length() == 1 && str.charAt(0) == singleCharacter);
	}

	/**
	 * Test if the given {@code String} starts with the specified prefix,
	 * ignoring upper/lower case.
	 *
	 * @param str    the {@code String} to check
	 * @param prefix the prefix to look for
	 * @see java.lang.String#startsWith
	 */
	public static boolean startsWithIgnoreCase(@Nullable String str, @Nullable String prefix) {
		return (str != null && prefix != null && str.length() >= prefix.length() &&
				str.regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	/**
	 * Test if the given {@code String} ends with the specified suffix,
	 * ignoring upper/lower case.
	 *
	 * @param str    the {@code String} to check
	 * @param suffix the suffix to look for
	 * @see java.lang.String#endsWith
	 */
	public static boolean endsWithIgnoreCase(@Nullable String str, @Nullable String suffix) {
		return (str != null && suffix != null && str.length() >= suffix.length() &&
				str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length()));
	}

	/**
	 * Test whether the given string matches the given substring
	 * at the given index.
	 *
	 * @param str       the original string (or StringBuilder)
	 * @param index     the index in the original string to start matching against
	 * @param substring the substring to match at the given index
	 */
	public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
		if (index + substring.length() > str.length()) {
			return false;
		}
		for (int i = 0; i < substring.length(); i++) {
			if (str.charAt(index + i) != substring.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Count the occurrences of the substring {@code sub} in string {@code str}.
	 *
	 * @param str string to search in
	 * @param sub string to search for
	 */
	public static int countOccurrencesOf(String str, String sub) {
		if (!hasLength(str) || !hasLength(sub)) {
			return 0;
		}

		int count = 0;
		int pos = 0;
		int idx;
		while ((idx = str.indexOf(sub, pos)) != -1) {
			++count;
			pos = idx + sub.length();
		}
		return count;
	}

	/**
	 * Delete all occurrences of the given substring.
	 *
	 * @param inString the original {@code String}
	 * @param pattern  the pattern to delete all occurrences of
	 * @return the resulting {@code String}
	 */
	public static String delete(String inString, String pattern) {
		return replace(inString, pattern, "");
	}

	/**
	 * Replace all occurrences of a substring within a string with another string.
	 *
	 * @param inString   {@code String} to examine
	 * @param oldPattern {@code String} to replace
	 * @param newPattern {@code String} to insert
	 * @return a {@code String} with the replacements
	 */
	public static String replace(String inString, String oldPattern, @Nullable String newPattern) {
		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
			return inString;
		}
		int index = inString.indexOf(oldPattern);
		if (index == -1) {
			// no occurrence -> can return input as-is
			return inString;
		}

		int capacity = inString.length();
		if (newPattern.length() > oldPattern.length()) {
			capacity += 16;
		}
		StringBuilder sb = new StringBuilder(capacity);

		int pos = 0;  // our position in the old string
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString, pos, index);
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}

		// append any characters to the right of a match
		sb.append(inString, pos, inString.length());
		return sb.toString();
	}

	/**
	 * Turn the given Object into a {@code String} with single quotes
	 * if it is a {@code String}; keeping the Object as-is else.
	 *
	 * @param obj the input Object (e.g. "myString")
	 * @return the quoted {@code String} (e.g. "'myString'"),
	 * or the input object as-is if not a {@code String}
	 */
	@Nullable
	public static Object quoteIfString(@Nullable Object obj) {
		return (obj instanceof String ? quote((String) obj) : obj);
	}

	/**
	 * Quote the given {@code String} with single quotes.
	 *
	 * @param str the input {@code String} (e.g. "myString")
	 * @return the quoted {@code String} (e.g. "'myString'"),
	 * or {@code null} if the input was {@code null}
	 */
	@Nullable
	public static String quote(@Nullable String str) {
		return (str != null ? "'" + str + "'" : null);
	}

	/**
	 * Unqualify a string qualified by a '.' dot character. For example,
	 * "this.name.is.qualified", returns "qualified".
	 *
	 * @param qualifiedName the qualified name
	 */
	public static String unqualify(String qualifiedName) {
		return unqualify(qualifiedName, '.');
	}

	/**
	 * Unqualify a string qualified by a separator character. For example,
	 * "this:name:is:qualified" returns "qualified" if using a ':' separator.
	 *
	 * @param qualifiedName the qualified name
	 * @param separator     the separator
	 */
	public static String unqualify(String qualifiedName, char separator) {
		return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
	}

	//---------------------------------------------------------------------
	// Convenience methods for working with formatted Strings
	//---------------------------------------------------------------------

	/**
	 * Capitalize a {@code String}, changing the first letter to
	 * upper case as per {@link Character#toUpperCase(char)}.
	 * No other letters are changed.
	 *
	 * @param str the {@code String} to capitalize
	 * @return the capitalized {@code String}
	 */
	public static String capitalize(String str) {
		return changeFirstCharacterCase(str, true);
	}

	private static String changeFirstCharacterCase(String str, boolean capitalize) {
		if (!hasLength(str)) {
			return str;
		}

		char baseChar = str.charAt(0);
		char updatedChar;
		if (capitalize) {
			updatedChar = Character.toUpperCase(baseChar);
		} else {
			updatedChar = Character.toLowerCase(baseChar);
		}
		if (baseChar == updatedChar) {
			return str;
		}

		char[] chars = str.toCharArray();
		chars[0] = updatedChar;
		return new String(chars);
	}

	/**
	 * Uncapitalize a {@code String}, changing the first letter to
	 * lower case as per {@link Character#toLowerCase(char)}.
	 * No other letters are changed.
	 *
	 * @param str the {@code String} to uncapitalize
	 * @return the uncapitalized {@code String}
	 */
	public static String uncapitalize(String str) {
		return changeFirstCharacterCase(str, false);
	}

	/**
	 * Extract the filename from the given Java resource path,
	 * e.g. {@code "mypath/myfile.txt" &rarr; "myfile.txt"}.
	 *
	 * @param path the file path (may be {@code null})
	 * @return the extracted filename, or {@code null} if none
	 */
	@Nullable
	public static String getFilename(@Nullable String path) {
		if (path == null) {
			return null;
		}

		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
	}

	/**
	 * Extract the filename extension from the given Java resource path,
	 * e.g. "mypath/myfile.txt" &rarr; "txt".
	 *
	 * @param path the file path (may be {@code null})
	 * @return the extracted filename extension, or {@code null} if none
	 */
	@Nullable
	public static String getFilenameExtension(@Nullable String path) {
		if (path == null) {
			return null;
		}

		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return null;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (folderIndex > extIndex) {
			return null;
		}

		return path.substring(extIndex + 1);
	}

	/**
	 * Strip the filename extension from the given Java resource path,
	 * e.g. "mypath/myfile.txt" &rarr; "mypath/myfile".
	 *
	 * @param path the file path
	 * @return the path with stripped filename extension
	 */
	public static String stripFilenameExtension(String path) {
		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return path;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (folderIndex > extIndex) {
			return path;
		}

		return path.substring(0, extIndex);
	}

	/**
	 * Apply the given relative path to the given Java resource path,
	 * assuming standard Java folder separation (i.e. "/" separators).
	 *
	 * @param path         the path to start from (usually a full file path)
	 * @param relativePath the relative path to apply
	 *                     (relative to the full file path above)
	 * @return the full file path that results from applying the relative path
	 */
	public static String applyRelativePath(String path, String relativePath) {
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (separatorIndex != -1) {
			String newPath = path.substring(0, separatorIndex);
			if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
				newPath += FOLDER_SEPARATOR;
			}
			return newPath + relativePath;
		} else {
			return relativePath;
		}
	}

	/**
	 * Compare two paths after normalization of them.
	 *
	 * @param path1 first path for comparison
	 * @param path2 second path for comparison
	 * @return whether the two paths are equivalent after normalization
	 */
	public static boolean pathEquals(String path1, String path2) {
		return cleanPath(path1).equals(cleanPath(path2));
	}

	/**
	 * Normalize the path by suppressing sequences like "path/.." and
	 * inner simple dots.
	 * <p>The result is convenient for path comparison. For other uses,
	 * notice that Windows separators ("\") are replaced by simple slashes.
	 * <p><strong>NOTE</strong> that {@code cleanPath} should not be depended
	 * upon in a security context. Other mechanisms should be used to prevent
	 * path-traversal issues.
	 *
	 * @param path the original path
	 * @return the normalized path
	 */
	public static String cleanPath(String path) {
		if (!hasLength(path)) {
			return path;
		}

		String normalizedPath = replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);
		String pathToUse = normalizedPath;

		// Shortcut if there is no work to do
		if (pathToUse.indexOf('.') == -1) {
			return pathToUse;
		}

		// Strip prefix from path to analyze, to not treat it as part of the
		// first path element. This is necessary to correctly parse paths like
		// "file:core/../core/io/Resource.class", where the ".." should just
		// strip the first "core" directory while keeping the "file:" prefix.
		int prefixIndex = pathToUse.indexOf(':');
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains(FOLDER_SEPARATOR)) {
				prefix = "";
			} else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
		// we never require more elements than pathArray and in the common case the same number
		Deque<String> pathElements = new ArrayDeque<>(pathArray.length);
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// Points to current directory - drop it.
			} else if (TOP_PATH.equals(element)) {
				// Registering top path found.
				tops++;
			} else {
				if (tops > 0) {
					// Merging path element with element corresponding to top path.
					tops--;
				} else {
					// Normal path element found.
					pathElements.addFirst(element);
				}
			}
		}

		// All path elements stayed the same - shortcut
		if (pathArray.length == pathElements.size()) {
			return normalizedPath;
		}
		// Remaining top paths need to be retained.
		for (int i = 0; i < tops; i++) {
			pathElements.addFirst(TOP_PATH);
		}
		// If nothing else left, at least explicitly point to current path.
		if (pathElements.size() == 1 && pathElements.getLast().isEmpty() && !prefix.endsWith(FOLDER_SEPARATOR)) {
			pathElements.addFirst(CURRENT_PATH);
		}

		final String joined = collectionToDelimitedString(pathElements, FOLDER_SEPARATOR);
		// avoid string concatenation with empty prefix
		return prefix.isEmpty() ? joined : prefix + joined;
	}

	/**
	 * Take a {@code String} that is a delimited list and convert it into a
	 * {@code String} array.
	 * <p>A single {@code delimiter} may consist of more than one character,
	 * but it will still be considered as a single delimiter string, rather
	 * than as bunch of potential delimiter characters, in contrast to
	 * {@link #tokenizeToStringArray}.
	 *
	 * @param str       the input {@code String} (potentially {@code null} or empty)
	 * @param delimiter the delimiter between elements (this is a single delimiter,
	 *                  rather than a bunch individual delimiter characters)
	 * @return an array of the tokens in the list
	 * @see #tokenizeToStringArray
	 */
	public static String[] delimitedListToStringArray(@Nullable String str, @Nullable String delimiter) {
		return delimitedListToStringArray(str, delimiter, null);
	}

	/**
	 * Convert a {@code Collection} into a delimited {@code String} (e.g. CSV).
	 * <p>Useful for {@code toString()} implementations.
	 *
	 * @param coll  the {@code Collection} to convert (potentially {@code null} or empty)
	 * @param delim the delimiter to use (typically a ",")
	 * @return the delimited {@code String}
	 */
	public static String collectionToDelimitedString(@Nullable Collection<?> coll, String delim) {
		return collectionToDelimitedString(coll, delim, "", "");
	}

	/**
	 * Take a {@code String} that is a delimited list and convert it into
	 * a {@code String} array.
	 * <p>A single {@code delimiter} may consist of more than one character,
	 * but it will still be considered as a single delimiter string, rather
	 * than as bunch of potential delimiter characters, in contrast to
	 * {@link #tokenizeToStringArray}.
	 *
	 * @param str           the input {@code String} (potentially {@code null} or empty)
	 * @param delimiter     the delimiter between elements (this is a single delimiter,
	 *                      rather than a bunch individual delimiter characters)
	 * @param charsToDelete a set of characters to delete; useful for deleting unwanted
	 *                      line breaks: e.g. "\r\n\f" will delete all new lines and line feeds in a {@code String}
	 * @return an array of the tokens in the list
	 * @see #tokenizeToStringArray
	 */
	public static String[] delimitedListToStringArray(
			@Nullable String str, @Nullable String delimiter, @Nullable String charsToDelete) {

		if (str == null) {
			return EMPTY_STRING_ARRAY;
		}
		if (delimiter == null) {
			return new String[]{str};
		}

		List<String> result = new ArrayList<>();
		if (delimiter.isEmpty()) {
			for (int i = 0; i < str.length(); i++) {
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		} else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {
				// Add rest of String, but not in case of empty input.
				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		return toStringArray(result);
	}

	/**
	 * Convert a {@link Collection} to a delimited {@code String} (e.g. CSV).
	 * <p>Useful for {@code toString()} implementations.
	 *
	 * @param coll   the {@code Collection} to convert (potentially {@code null} or empty)
	 * @param delim  the delimiter to use (typically a ",")
	 * @param prefix the {@code String} to start each element with
	 * @param suffix the {@code String} to end each element with
	 * @return the delimited {@code String}
	 */
	public static String collectionToDelimitedString(
			@Nullable Collection<?> coll, String delim, String prefix, String suffix) {

		if (CollectionUtils.isEmpty(coll)) {
			return "";
		}

		int totalLength = coll.size() * (prefix.length() + suffix.length()) + (coll.size() - 1) * delim.length();
		for (Object element : coll) {
			totalLength += String.valueOf(element).length();
		}

		StringBuilder sb = new StringBuilder(totalLength);
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append(prefix).append(it.next()).append(suffix);
			if (it.hasNext()) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

	/**
	 * Delete any character in a given {@code String}.
	 *
	 * @param inString      the original {@code String}
	 * @param charsToDelete a set of characters to delete.
	 *                      E.g. "az\n" will delete 'a's, 'z's and new lines.
	 * @return the resulting {@code String}
	 */
	public static String deleteAny(String inString, @Nullable String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			return inString;
		}

		int lastCharIndex = 0;
		char[] result = new char[inString.length()];
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				result[lastCharIndex++] = c;
			}
		}
		if (lastCharIndex == inString.length()) {
			return inString;
		}
		return new String(result, 0, lastCharIndex);
	}

	/**
	 * Copy the given {@link Collection} into a {@code String} array.
	 * <p>The {@code Collection} must contain {@code String} elements only.
	 *
	 * @param collection the {@code Collection} to copy
	 *                   (potentially {@code null} or empty)
	 * @return the resulting {@code String} array
	 */
	public static String[] toStringArray(@Nullable Collection<String> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_STRING_ARRAY) : EMPTY_STRING_ARRAY);
	}

	/**
	 * Decode the given encoded URI component value. Based on the following rules:
	 * <ul>
	 * <li>Alphanumeric characters {@code "a"} through {@code "z"}, {@code "A"} through {@code "Z"},
	 * and {@code "0"} through {@code "9"} stay the same.</li>
	 * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and {@code "*"} stay the same.</li>
	 * <li>A sequence "{@code %<i>xy</i>}" is interpreted as a hexadecimal representation of the character.</li>
	 * </ul>
	 *
	 * @param source  the encoded String
	 * @param charset the character set
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @see java.net.URLDecoder#decode(String, String)
	 * @since 5.0
	 */
	public static String uriDecode(String source, Charset charset) {
		int length = source.length();
		if (length == 0) {
			return source;
		}
		Assert.notNull(charset, "Charset must not be null");

		ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
		boolean changed = false;
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if (i + 2 < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					baos.write((char) ((u << 4) + l));
					i += 2;
					changed = true;
				} else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			} else {
				baos.write(ch);
			}
		}
		return (changed ? StreamUtils.copyToString(baos, charset) : source);
	}

	/**
	 * Parse the given {@code String} value into a {@link Locale}, accepting
	 * the {@link Locale#toString} format as well as BCP 47 language tags.
	 *
	 * @param localeValue the locale value: following either {@code Locale's}
	 *                    {@code toString()} format ("en", "en_UK", etc), also accepting spaces as
	 *                    separators (as an alternative to underscores), or BCP 47 (e.g. "en-UK")
	 *                    as specified by {@link Locale#forLanguageTag} on Java 7+
	 * @return a corresponding {@code Locale} instance, or {@code null} if none
	 * @throws IllegalArgumentException in case of an invalid locale specification
	 * @see #parseLocaleString
	 * @see Locale#forLanguageTag
	 * @since 5.0.4
	 */
	@Nullable
	public static Locale parseLocale(String localeValue) {
		String[] tokens = tokenizeLocaleSource(localeValue);
		if (tokens.length == 1) {
			validateLocalePart(localeValue);
			Locale resolved = Locale.forLanguageTag(localeValue);
			if (resolved.getLanguage().length() > 0) {
				return resolved;
			}
		}
		return parseLocaleTokens(localeValue, tokens);
	}

	private static String[] tokenizeLocaleSource(String localeSource) {
		return tokenizeToStringArray(localeSource, "_ ", false, false);
	}

	private static void validateLocalePart(String localePart) {
		for (int i = 0; i < localePart.length(); i++) {
			char ch = localePart.charAt(i);
			if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
				throw new IllegalArgumentException(
						"Locale part \"" + localePart + "\" contains invalid characters");
			}
		}
	}

	@Nullable
	private static Locale parseLocaleTokens(String localeString, String[] tokens) {
		String language = (tokens.length > 0 ? tokens[0] : "");
		String country = (tokens.length > 1 ? tokens[1] : "");
		validateLocalePart(language);
		validateLocalePart(country);

		String variant = "";
		if (tokens.length > 2) {
			// There is definitely a variant, and it is everything after the country
			// code sans the separator between the country code and the variant.
			int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();
			// Strip off any leading '_' and whitespace, what's left is the variant.
			variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
			if (variant.startsWith("_")) {
				variant = trimLeadingCharacter(variant, '_');
			}
		}

		if (variant.isEmpty() && country.startsWith("#")) {
			variant = country;
			country = "";
		}

		return (language.length() > 0 ? new Locale(language, country, variant) : null);
	}

	/**
	 * Tokenize the given {@code String} into a {@code String} array via a
	 * {@link StringTokenizer}.
	 * <p>The given {@code delimiters} string can consist of any number of
	 * delimiter characters. Each of those characters can be used to separate
	 * tokens. A delimiter is always a single character; for multi-character
	 * delimiters, consider using {@link #delimitedListToStringArray}.
	 *
	 * @param str               the {@code String} to tokenize (potentially {@code null} or empty)
	 * @param delimiters        the delimiter characters, assembled as a {@code String}
	 *                          (each of the characters is individually considered as a delimiter)
	 * @param trimTokens        trim the tokens via {@link String#trim()}
	 * @param ignoreEmptyTokens omit empty tokens from the result array
	 *                          (only applies to tokens that are empty after trimming; StringTokenizer
	 *                          will not consider subsequent delimiters as token in the first place).
	 * @return an array of the tokens
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(
			@Nullable String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (str == null) {
			return EMPTY_STRING_ARRAY;
		}

		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}


	//---------------------------------------------------------------------
	// Convenience methods for working with String arrays
	//---------------------------------------------------------------------

	/**
	 * Trim leading whitespace from the given {@code String}.
	 *
	 * @param str the {@code String} to check
	 * @return the trimmed {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimLeadingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIdx = 0;
		while (beginIdx < str.length() && Character.isWhitespace(str.charAt(beginIdx))) {
			beginIdx++;
		}
		return str.substring(beginIdx);
	}

	/**
	 * Trim all occurrences of the supplied leading character from the given {@code String}.
	 *
	 * @param str              the {@code String} to check
	 * @param leadingCharacter the leading character to be trimmed
	 * @return the trimmed {@code String}
	 */
	public static String trimLeadingCharacter(String str, char leadingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIdx = 0;
		while (beginIdx < str.length() && leadingCharacter == str.charAt(beginIdx)) {
			beginIdx++;
		}
		return str.substring(beginIdx);
	}

	/**
	 * Parse the given {@code String} representation into a {@link Locale}.
	 * <p>For many parsing scenarios, this is an inverse operation of
	 * {@link Locale#toString Locale's toString}, in a lenient sense.
	 * This method does not aim for strict {@code Locale} design compliance;
	 * it is rather specifically tailored for typical Spring parsing needs.
	 * <p><b>Note: This delegate does not accept the BCP 47 language tag format.
	 * Please use {@link #parseLocale} for lenient parsing of both formats.</b>
	 *
	 * @param localeString the locale {@code String}: following {@code Locale's}
	 *                     {@code toString()} format ("en", "en_UK", etc), also accepting spaces as
	 *                     separators (as an alternative to underscores)
	 * @return a corresponding {@code Locale} instance, or {@code null} if none
	 * @throws IllegalArgumentException in case of an invalid locale specification
	 */
	@Nullable
	public static Locale parseLocaleString(String localeString) {
		return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
	}

	/**
	 * Determine the RFC 3066 compliant language tag,
	 * as used for the HTTP "Accept-Language" header.
	 *
	 * @param locale the Locale to transform to a language tag
	 * @return the RFC 3066 compliant language tag as {@code String}
	 * @deprecated as of 5.0.4, in favor of {@link Locale#toLanguageTag()}
	 */
	@Deprecated
	public static String toLanguageTag(Locale locale) {
		return locale.getLanguage() + (hasText(locale.getCountry()) ? "-" + locale.getCountry() : "");
	}

	/**
	 * Check whether the given {@code String} contains actual <em>text</em>.
	 * <p>More specifically, this method returns {@code true} if the
	 * {@code String} is not {@code null}, its length is greater than 0,
	 * and it contains at least one non-whitespace character.
	 *
	 * @param str the {@code String} to check (may be {@code null})
	 * @return {@code true} if the {@code String} is not {@code null}, its
	 * length is greater than 0, and it does not contain whitespace only
	 * @see #hasText(CharSequence)
	 * @see #hasLength(String)
	 * @see Character#isWhitespace
	 */
	public static boolean hasText(@Nullable String str) {
		return (str != null && !str.isEmpty() && containsText(str));
	}

	/**
	 * Parse the given {@code timeZoneString} value into a {@link TimeZone}.
	 *
	 * @param timeZoneString the time zone {@code String}, following {@link TimeZone#getTimeZone(String)}
	 *                       but throwing {@link IllegalArgumentException} in case of an invalid time zone specification
	 * @return a corresponding {@link TimeZone} instance
	 * @throws IllegalArgumentException in case of an invalid time zone specification
	 */
	public static TimeZone parseTimeZoneString(String timeZoneString) {
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
		if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
			// We don't want that GMT fallback...
			throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
		}
		return timeZone;
	}

	/**
	 * Copy the given {@link Enumeration} into a {@code String} array.
	 * <p>The {@code Enumeration} must contain {@code String} elements only.
	 *
	 * @param enumeration the {@code Enumeration} to copy
	 *                    (potentially {@code null} or empty)
	 * @return the resulting {@code String} array
	 */
	public static String[] toStringArray(@Nullable Enumeration<String> enumeration) {
		return (enumeration != null ? toStringArray(Collections.list(enumeration)) : EMPTY_STRING_ARRAY);
	}

	/**
	 * Append the given {@code String} to the given {@code String} array,
	 * returning a new array consisting of the input array contents plus
	 * the given {@code String}.
	 *
	 * @param array the array to append to (can be {@code null})
	 * @param str   the {@code String} to append
	 * @return the new array (never {@code null})
	 */
	public static String[] addStringToArray(@Nullable String[] array, String str) {
		if (ObjectUtils.isEmpty(array)) {
			return new String[]{str};
		}

		String[] newArr = new String[array.length + 1];
		System.arraycopy(array, 0, newArr, 0, array.length);
		newArr[array.length] = str;
		return newArr;
	}

	/**
	 * Concatenate the given {@code String} arrays into one,
	 * with overlapping array elements included twice.
	 * <p>The order of elements in the original arrays is preserved.
	 *
	 * @param array1 the first array (can be {@code null})
	 * @param array2 the second array (can be {@code null})
	 * @return the new array ({@code null} if both given arrays were {@code null})
	 */
	@Nullable
	public static String[] concatenateStringArrays(@Nullable String[] array1, @Nullable String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		String[] newArr = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, newArr, 0, array1.length);
		System.arraycopy(array2, 0, newArr, array1.length, array2.length);
		return newArr;
	}

	/**
	 * Merge the given {@code String} arrays into one, with overlapping
	 * array elements only included once.
	 * <p>The order of elements in the original arrays is preserved
	 * (with the exception of overlapping elements, which are only
	 * included on their first occurrence).
	 *
	 * @param array1 the first array (can be {@code null})
	 * @param array2 the second array (can be {@code null})
	 * @return the new array ({@code null} if both given arrays were {@code null})
	 * @deprecated as of 4.3.15, in favor of manual merging via {@link LinkedHashSet}
	 * (with every entry included at most once, even entries within the first array)
	 */
	@Deprecated
	@Nullable
	public static String[] mergeStringArrays(@Nullable String[] array1, @Nullable String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		List<String> result = new ArrayList<>(Arrays.asList(array1));
		for (String str : array2) {
			if (!result.contains(str)) {
				result.add(str);
			}
		}
		return toStringArray(result);
	}

	/**
	 * Sort the given {@code String} array if necessary.
	 *
	 * @param array the original array (potentially empty)
	 * @return the array in sorted form (never {@code null})
	 */
	public static String[] sortStringArray(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		Arrays.sort(array);
		return array;
	}

	/**
	 * Trim the elements of the given {@code String} array, calling
	 * {@code String.trim()} on each non-null element.
	 *
	 * @param array the original {@code String} array (potentially empty)
	 * @return the resulting array (of the same size) with trimmed elements
	 */
	public static String[] trimArrayElements(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			String element = array[i];
			result[i] = (element != null ? element.trim() : null);
		}
		return result;
	}

	/**
	 * Remove duplicate strings from the given array.
	 * <p>As of 4.2, it preserves the original order, as it uses a {@link LinkedHashSet}.
	 *
	 * @param array the {@code String} array (potentially empty)
	 * @return an array without duplicates, in natural sort order
	 */
	public static String[] removeDuplicateStrings(String[] array) {
		// 如果传入的数组为空，则直接返回
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		// 使用LinkedHashSet保存传入数组中的元素，LinkedHashSet可以保证元素的唯一性和插入顺序
		Set<String> set = new LinkedHashSet<>(Arrays.asList(array));
		// 将Set转换为String数组
		return toStringArray(set);
	}

	/**
	 * 取一个字符串数组，并且在每个元素上根据给定的分隔符进行分割。
	 * 一个{@code Properties}实例被创建，左边的分隔符前的部分作为键，
	 * 右边的分隔符后的部分作为值。
	 * <p>在将键和值添加到{@code Properties}之前，将它们都进行修剪。
	 *
	 * @param array     要处理的数组
	 * @param delimiter 用于分割每个元素的符号（通常是等号）
	 * @return 一个代表数组内容的{@code Properties}实例，
	 * 如果要处理的数组为{@code null}或空，则返回{@code null}
	 */
	@Nullable
	public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
		return splitArrayElementsIntoProperties(array, delimiter, null);
	}


	/**
	 * 将字符串数组根据给定的分隔符拆分每个元素。
	 * 然后生成一个 {@code Properties} 实例，分隔符左侧作为键，右侧作为值。
	 * <p>在将键和值添加到 {@code Properties} 实例之前，将对它们进行修剪。
	 *
	 * @param array         要处理的数组
	 * @param delimiter     用于拆分每个元素的分隔符（通常是等号符号）
	 * @param charsToDelete 要从每个元素中删除的一个或多个字符
	 *                      在尝试拆分操作之前（通常是引号符号），如果不需要删除，则为 {@code null}
	 * @return 代表数组内容的 {@code Properties} 实例，
	 * 如果要处理的数组为 {@code null} 或为空，则返回 {@code null}
	 */
	@Nullable
	public static Properties splitArrayElementsIntoProperties(
			String[] array, String delimiter, @Nullable String charsToDelete) {

		if (ObjectUtils.isEmpty(array)) {
			return null;
		}

		Properties result = new Properties();
		for (String element : array) {
			if (charsToDelete != null) {
				element = deleteAny(element, charsToDelete);
			}
			String[] splittedElement = split(element, delimiter);
			if (splittedElement == null) {
				continue;
			}
			result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
		}
		return result;
	}

	/**
	 * 在第一次出现分隔符的位置拆分一个 {@code String}。
	 * 结果中不包括分隔符。
	 *
	 * @param toSplit   要拆分的字符串（可能为 {@code null} 或空）
	 * @param delimiter 用于拆分字符串的分隔符（可能为 {@code null} 或空）
	 * @return 包含两个元素的数组，索引 0 是分隔符之前的部分，
	 * 索引 1 是分隔符之后的部分（两个元素都不包括分隔符）；
	 * 如果在给定的输入 {@code String} 中未找到分隔符，则返回 {@code null}
	 */
	@Nullable
	public static String[] split(@Nullable String toSplit, @Nullable String delimiter) {
		// 如果 toSplit 或 delimiter 的长度为 0，则返回 null
		if (!hasLength(toSplit) || !hasLength(delimiter)) {
			return null;
		}
		// 查找分隔符在 toSplit 中第一次出现的位置
		int offset = toSplit.indexOf(delimiter);
		// 如果未找到，则返回 null
		if (offset < 0) {
			return null;
		}

		// 截取分隔符之前的字符串
		String beforeDelimiter = toSplit.substring(0, offset);
		// 截取分隔符之后的字符串
		String afterDelimiter = toSplit.substring(offset + delimiter.length());
		// 返回一个包含两个元素的数组，第一个元素是分隔符之前的部分，第二个元素是分隔符之后的部分
		return new String[]{beforeDelimiter, afterDelimiter};
	}

	/**
	 * 将给定的{@code String}通过{@link StringTokenizer}分割成{@code String}数组。
	 * <p>修剪令牌并省略空令牌。
	 * <p>给定的{@code delimiters}字符串可以包含任意数量的分割符字符。
	 * 每个这些字符都可以用于分离令牌。分隔符总是单个字符；对于多字符分隔符，
	 * 请考虑使用{@link #delimitedListToStringArray}。
	 *
	 * @param str        要分割的{@code String}（可能是{@code null}或空）
	 * @param delimiters 分隔符字符，组装成一个{@code String}
	 *                   (每个字符都被视为分隔符)
	 * @return 分隔符的数组
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(@Nullable String str, String delimiters) {
		return tokenizeToStringArray(str, delimiters, true, true);
	}

	/**
	 * 将逗号分隔的列表（例如CSV文件中的一行）转换为集合。
	 * <p>注意这将抑制重复项，并且在4.2版本中，返回的集合中的元素将保留原始顺序在{@link LinkedHashSet}中。
	 *
	 * @param str 输入的{@code String}（可能是{@code null}或空）
	 * @return 一个{@code String}元素的集合
	 * @see #removeDuplicateStrings(String[])
	 */
	public static Set<String> commaDelimitedListToSet(@Nullable String str) {
		String[] tokens = commaDelimitedListToStringArray(str);
		return new LinkedHashSet<>(Arrays.asList(tokens));
	}

	/**
	 * 将逗号分隔的列表（例如CSV文件中的一行）转换为字符串数组。
	 *
	 * @param str 输入的{@code String}（可能是{@code null}或空）
	 * @return 一个字符串数组，或者在空输入的情况下返回空数组
	 */
	public static String[] commaDelimitedListToStringArray(@Nullable String str) {
		return delimitedListToStringArray(str, ",");
	}

	/**
	 * Convert a {@code Collection} into a delimited {@code String} (e.g., CSV).
	 * <p>Useful for {@code toString()} implementations.
	 *
	 * @param coll the {@code Collection} to convert (potentially {@code null} or empty)
	 * @return the delimited {@code String}
	 */
	public static String collectionToCommaDelimitedString(@Nullable Collection<?> coll) {
		return collectionToDelimitedString(coll, ",");
	}

	/**
	 * Convert a {@code String} array into a comma delimited {@code String}
	 * (i.e., CSV).
	 * <p>Useful for {@code toString()} implementations.
	 *
	 * @param arr the array to display (potentially {@code null} or empty)
	 * @return the delimited {@code String}
	 */
	public static String arrayToCommaDelimitedString(@Nullable Object[] arr) {
		return arrayToDelimitedString(arr, ",");
	}

	/**
	 * Convert a {@code String} array into a delimited {@code String} (e.g. CSV).
	 * <p>Useful for {@code toString()} implementations.
	 *
	 * @param arr   the array to display (potentially {@code null} or empty)
	 * @param delim the delimiter to use (typically a ",")
	 * @return the delimited {@code String}
	 */
	public static String arrayToDelimitedString(@Nullable Object[] arr, String delim) {
		// 如果 arr 为空，则返回空字符串
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		}
		// 如果 arr 长度为 1，则返回 arr[0] 的字符串表示
		if (arr.length == 1) {
			return ObjectUtils.nullSafeToString(arr[0]);
		}

		// 创建 StringJoiner 对象，用于拼接字符串
		StringJoiner sj = new StringJoiner(delim);
		// 遍历 arr，将每个元素转换为字符串，并添加到 StringJoiner 中
		for (Object elem : arr) {
			sj.add(String.valueOf(elem));
		}
		// 返回拼接后的字符串
		return sj.toString();
	}

}
