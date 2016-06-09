package com.buhtum.sonshonjmo;

import com.google.common.collect.ImmutableSet;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * This test class shows how to write unit tests with TestNG, Mockito and FEST-Assert 2.
 * 
 * Don't pay too much attention to the semantics of the actual tests.
 */
public class BobRossTest {

    private static final Set<String> ANY_PAINTING_ELEMENTS = ImmutableSet.of("sky", "mountain", "happy tree");
    private static final Set<String> ANY_PAINTING_ELEMENTS_BUT_NO_HAPPY_TREE = ImmutableSet.of("sky", "mountain");

    /**
     * Shows using mocks
     */
    @Test
    public void shouldCommunicateWhenPainting() {
        // given
    }

    /**
     * Shows testing for expected exceptions
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldReturnImmutableSetOfPaintingElements() {
        // given

        // when

        // then
    }

    @DataProvider
    public Object[][] paintingElementsWithoutHappyTreeData() {
        return new Object[][] {
                { ImmutableSet.of("sky", "mountain", "cloud") },
                { ImmutableSet.of("sky", "lake", "barn", "squirrel") } };
    }

    /**
     * Shows the use of data providers in TestNG and of assertThat() in FEST-Assert 2.x
     */
    @Test(dataProvider = "paintingElementsWithoutHappyTreeData")
    public void shouldAlwaysPaintAHappyTree(Set<String> paintingElementsWithoutHappyTree) {
        // given

        // when

        // then
    }
}
