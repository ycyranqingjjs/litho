/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.assertj.LithoViewAssert;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowView;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.litho.ComponentTree.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

@RunWith(ComponentsTestRunner.class)
public class LithoViewTest {
  private LithoView mLithoView;

  @Rule
  public ExpectedException mExpectedException = ExpectedException.none();

  @Before
  public void setup() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return TestDrawableComponent.create(c)
            .withLayout()
            .widthPx(100)
            .heightPx(100)
            .build();
      }
    };

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final ComponentTree componentTree = ComponentTree.create(c, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    mLithoView = new LithoView(RuntimeEnvironment.application);
    mLithoView.setComponentTree(componentTree);
  }

  @Test
  public void measureBeforeBeingAttached() {
    mLithoView.measure(
        makeMeasureSpec(0, UNSPECIFIED),
        makeMeasureSpec(0, UNSPECIFIED));
    mLithoView.layout(
        0,
        0,
        mLithoView.getMeasuredWidth(),
        mLithoView.getMeasuredHeight());

    // View got measured.
    assertThat(mLithoView.getMeasuredHeight()).isGreaterThan(0);
    assertThat(mLithoView.getMeasuredHeight()).isGreaterThan(0);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertThat(getInternalMountItems(mLithoView)).hasSize(2);
  }

  private static long[] getInternalMountItems(LithoView lithoView) {
    MountState mountState = Whitebox.getInternalState(lithoView, "mMountState");
    return Whitebox.getInternalState(mountState, "mLayoutOutputsIds");
  }

  @Test
  public void testNullLithoViewDimensions() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return null;
      }
    };

    LithoView nullLithoView = new LithoView(application);
    nullLithoView.setComponentTree(
        create(
            new ComponentContext(application),
            component)
            .incrementalMount(false)
            .build());

    nullLithoView.measure(
        makeMeasureSpec(0, UNSPECIFIED),
        makeMeasureSpec(0, UNSPECIFIED));
    nullLithoView.layout(
        0,
        0,
        nullLithoView.getMeasuredWidth(),
        nullLithoView.getMeasuredHeight());

    LithoViewAssert.assertThat(nullLithoView)
        .hasMeasuredWidthOf(0)
        .hasMeasuredHeightOf(0);
  }

  @Test
  public void testSuppressMeasureComponentTree() {
    final ComponentTree mockComponentTree = mock(ComponentTree.class);
    final int width = 240;
    final int height = 400;

    mLithoView.setComponentTree(mockComponentTree);
    mLithoView.suppressMeasureComponentTree(true);
    mLithoView.measure(
        makeMeasureSpec(width, EXACTLY),
        makeMeasureSpec(height, EXACTLY));

    verify(mockComponentTree, never())
        .measure(anyInt(), anyInt(), any(int[].class), anyBoolean());
    LithoViewAssert.assertThat(mLithoView)
        .hasMeasuredWidthOf(width)
        .hasMeasuredHeightOf(height);
  }

  @Test
  public void testThrowWhenMainThreadLayoutStateIsNullAndLayoutNotRequested() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage("hasn't requested layout");

    final ComponentTree mockComponentTree = mock(ComponentTree.class);

    mLithoView.setComponentTree(mockComponentTree);

    // Clear requestLayout flag
    mLithoView.layout(0, 0, 100, 100);

    mLithoView.performIncrementalMount();
  }

  @Test
  public void testDontThrowWhenLayoutStateIsNull() {
    final ComponentTree mockComponentTree = mock(ComponentTree.class);

    mLithoView.setComponentTree(mockComponentTree);
    mLithoView.requestLayout();
    mLithoView.performIncrementalMount();
  }
}
