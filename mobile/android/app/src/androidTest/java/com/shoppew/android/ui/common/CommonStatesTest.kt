package com.shoppew.android.ui.common

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shoppew.android.ui.theme.ShoppewTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CommonStatesTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun recoverableErrorExposesMessageAndRetryAction() {
        var retried = false
        composeRule.setContent {
            ShoppewTheme {
                ErrorState("Máy chủ tạm thời chưa phản hồi", onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText("Máy chủ tạm thời chưa phản hồi").assertIsDisplayed()
        composeRule.onNodeWithText("Thử lại").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun emptyStateProvidesUsefulNextAction() {
        composeRule.setContent {
            ShoppewTheme {
                EmptyState("Chưa có đơn hàng", "Đơn mới sẽ xuất hiện tại đây.", "Khám phá sản phẩm") {}
            }
        }

        composeRule.onNodeWithText("Chưa có đơn hàng").assertIsDisplayed()
        composeRule.onNodeWithText("Khám phá sản phẩm").assertHasClickAction()
    }

    @Test
    fun offlineBannerCommunicatesCachedBrowsingSemantically() {
        composeRule.setContent {
            ShoppewTheme { NetworkStatusBanner() }
        }

        composeRule.onNodeWithContentDescription("Đang ngoại tuyến; nội dung sản phẩm đã lưu vẫn có thể xem")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Đang ngoại tuyến · hiển thị dữ liệu sản phẩm đã lưu").assertIsDisplayed()
    }
}
