/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.personalization;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.android.inputmethod.latin.ExpandableBinaryDictionary;
import com.android.inputmethod.latin.utils.BinaryDictionaryUtils;

import java.io.File;
import java.util.Locale;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for UserHistoryDictionary
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class UserHistoryDictionaryTests {
    private static final String TAG = UserHistoryDictionaryTests.class.getSimpleName();
    private static final int WAIT_FOR_WRITING_FILE_IN_MILLISECONDS = 3000;
    private static final String TEST_ACCOUNT = "account@example.com";

    private int mCurrentTime = 0;

    private Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    private static void printAllFiles(final File dir) {
        Log.d(TAG, dir.getAbsolutePath());
        for (final File file : dir.listFiles()) {
            Log.d(TAG, "  " + file.getName());
        }
    }

    private static void assertDictionaryExists(final UserHistoryDictionary dict,
            final File dictFile) {
        Log.d(TAG, "waiting for writing ...");
        dict.waitAllTasksForTests();
        if (!dictFile.exists()) {
            try {
                Log.d(TAG, dictFile + " is not existing. Wait "
                        + WAIT_FOR_WRITING_FILE_IN_MILLISECONDS + " ms for writing.");
                printAllFiles(dictFile.getParentFile());
                Thread.sleep(WAIT_FOR_WRITING_FILE_IN_MILLISECONDS);
            } catch (final InterruptedException e) {
                Log.e(TAG, "Interrupted during waiting for writing the dict file.");
            }
        }
        assertTrue("Following dictionary file doesn't exist: " + dictFile, dictFile.exists());
    }

    @Before
    public void setUp() throws Exception {
        resetCurrentTimeForTestMode();
        UserHistoryDictionaryTestsHelper.removeAllTestDictFiles(
                UserHistoryDictionaryTestsHelper.TEST_LOCALE_PREFIX, getContext());
    }

    @After
    public void tearDown() throws Exception {
        UserHistoryDictionaryTestsHelper.removeAllTestDictFiles(
                UserHistoryDictionaryTestsHelper.TEST_LOCALE_PREFIX, getContext());
        stopTestModeInNativeCode();
    }

    private void resetCurrentTimeForTestMode() {
        mCurrentTime = 0;
        setCurrentTimeForTestMode(mCurrentTime);
    }

    private static int setCurrentTimeForTestMode(final int currentTime) {
        return BinaryDictionaryUtils.setCurrentTimeForTest(currentTime);
    }

    private static int stopTestModeInNativeCode() {
        return BinaryDictionaryUtils.setCurrentTimeForTest(-1);
    }

    /**
     * Clear all entries in the user history dictionary.
     * @param dict the user history dictionary.
     */
    private static void clearHistory(final UserHistoryDictionary dict) {
        dict.waitAllTasksForTests();
        dict.clear();
        dict.close();
        dict.waitAllTasksForTests();
    }

    private void doTestRandomWords(final String testAccount) {
        Log.d(TAG, "This test can be used for profiling.");
        Log.d(TAG, "Usage: please set UserHistoryDictionary.PROFILE_SAVE_RESTORE to true.");
        final Locale dummyLocale = UserHistoryDictionaryTestsHelper.getDummyLocale("random_words");
        final String dictName = UserHistoryDictionary.getUserHistoryDictName(
                UserHistoryDictionary.NAME, dummyLocale,
                null /* dictFile */,
                testAccount /* account */);
        final File dictFile = ExpandableBinaryDictionary.getDictFile(
                getContext(), dictName, null /* dictFile */);
        final UserHistoryDictionary dict = PersonalizationHelper.getUserHistoryDictionary(
                getContext(), dummyLocale, testAccount);
        clearHistory(dict);

        final int numberOfWords = 1000;
        final Random random = new Random(123456);
        assertTrue(UserHistoryDictionaryTestsHelper.addAndWriteRandomWords(
                dict, numberOfWords, random, true /* checksContents */, mCurrentTime));
        assertDictionaryExists(dict, dictFile);
    }

    @Test
    public void testRandomWords_NullAccount() {
        doTestRandomWords(null /* testAccount */);
    }

    @Test
    public void testRandomWords() {
        doTestRandomWords(TEST_ACCOUNT);
    }

    @Test
    public void testStressTestForSwitchingLanguagesAndAddingWords() {
        doTestStressTestForSwitchingLanguagesAndAddingWords(TEST_ACCOUNT);
    }

    @Test
    public void testStressTestForSwitchingLanguagesAndAddingWords_NullAccount() {
        doTestStressTestForSwitchingLanguagesAndAddingWords(null /* testAccount */);
    }

    private void doTestStressTestForSwitchingLanguagesAndAddingWords(final String testAccount) {
        final int numberOfLanguages = 2;
        final int numberOfLanguageSwitching = 80;
        final int numberOfWordsInsertedForEachLanguageSwitch = 100;

        final File dictFiles[] = new File[numberOfLanguages];
        final UserHistoryDictionary dicts[] = new UserHistoryDictionary[numberOfLanguages];

        try {
            final Random random = new Random(123456);

            // Create filename suffixes for this test.
            for (int i = 0; i < numberOfLanguages; i++) {
                final Locale dummyLocale =
                        UserHistoryDictionaryTestsHelper.getDummyLocale("switching_languages" + i);
                final String dictName = UserHistoryDictionary.getUserHistoryDictName(
                        UserHistoryDictionary.NAME, dummyLocale, null /* dictFile */,
                        testAccount /* account */);
                dictFiles[i] = ExpandableBinaryDictionary.getDictFile(
                        getContext(), dictName, null /* dictFile */);
                dicts[i] = PersonalizationHelper.getUserHistoryDictionary(getContext(),
                        dummyLocale, testAccount);
                clearHistory(dicts[i]);
            }

            final long start = System.currentTimeMillis();

            for (int i = 0; i < numberOfLanguageSwitching; i++) {
                final int index = i % numberOfLanguages;
                // Switch to dicts[index].
                assertTrue(UserHistoryDictionaryTestsHelper.addAndWriteRandomWords(dicts[index],
                        numberOfWordsInsertedForEachLanguageSwitch,
                        random,
                        false /* checksContents */,
                        mCurrentTime));
            }

            final long end = System.currentTimeMillis();
            Log.d(TAG, "testStressTestForSwitchingLanguageAndAddingWords took "
                    + (end - start) + " ms");
        } finally {
            for (int i = 0; i < numberOfLanguages; i++) {
                assertDictionaryExists(dicts[i], dictFiles[i]);
            }
        }
    }

    @Test
    public void testAddManyWords() {
        doTestAddManyWords(TEST_ACCOUNT);
    }

    @Test
    public void testAddManyWords_NullAccount() {
        doTestAddManyWords(null /* testAccount */);
    }

    private void doTestAddManyWords(final String testAccount) {
        final Locale dummyLocale =
                UserHistoryDictionaryTestsHelper.getDummyLocale("many_random_words");
        final String dictName = UserHistoryDictionary.getUserHistoryDictName(
                UserHistoryDictionary.NAME, dummyLocale, null /* dictFile */, testAccount);
        final File dictFile = ExpandableBinaryDictionary.getDictFile(
                getContext(), dictName, null /* dictFile */);
        final int numberOfWords = 10000;
        final Random random = new Random(123456);
        final UserHistoryDictionary dict = PersonalizationHelper.getUserHistoryDictionary(
                getContext(), dummyLocale, testAccount);
        clearHistory(dict);
        assertTrue(UserHistoryDictionaryTestsHelper.addAndWriteRandomWords(dict,
                numberOfWords, random, true /* checksContents */, mCurrentTime));
        assertDictionaryExists(dict, dictFile);
    }
}