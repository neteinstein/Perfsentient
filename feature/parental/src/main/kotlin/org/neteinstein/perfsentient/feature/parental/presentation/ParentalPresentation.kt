package org.neteinstein.perfsentient.feature.parental.presentation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Masks
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.ui.graphics.vector.ImageVector
import org.neteinstein.perfsentient.core.model.ParentalConcern
import org.neteinstein.perfsentient.core.model.ParentalRiskCategory
import org.neteinstein.perfsentient.feature.parental.R

/** Icon and copy for a [ParentalRiskCategory]: what the app is, why it matters and what a parent can do. */
internal data class CategoryPresentation(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val guidance: Int,
)

/** Icon and copy for a [ParentalConcern] chip. */
internal data class ConcernPresentation(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

internal val ParentalRiskCategory.presentation: CategoryPresentation
    get() = when (this) {
        ParentalRiskCategory.GAMES_WITH_CHAT -> CategoryPresentation(
            Icons.Rounded.SportsEsports,
            R.string.parental_category_games_title,
            R.string.parental_category_games_desc,
            R.string.parental_category_games_guide,
        )

        ParentalRiskCategory.VIRTUAL_WORLDS -> CategoryPresentation(
            Icons.Rounded.ViewInAr,
            R.string.parental_category_worlds_title,
            R.string.parental_category_worlds_desc,
            R.string.parental_category_worlds_guide,
        )

        ParentalRiskCategory.SOCIAL_MEDIA -> CategoryPresentation(
            Icons.Rounded.DynamicFeed,
            R.string.parental_category_social_title,
            R.string.parental_category_social_desc,
            R.string.parental_category_social_guide,
        )

        ParentalRiskCategory.PRIVATE_MESSAGING -> CategoryPresentation(
            Icons.Rounded.Forum,
            R.string.parental_category_messaging_title,
            R.string.parental_category_messaging_desc,
            R.string.parental_category_messaging_guide,
        )

        ParentalRiskCategory.FRIEND_FINDING -> CategoryPresentation(
            Icons.Rounded.PersonSearch,
            R.string.parental_category_friend_finding_title,
            R.string.parental_category_friend_finding_desc,
            R.string.parental_category_friend_finding_guide,
        )

        ParentalRiskCategory.RANDOM_VIDEO_CHAT -> CategoryPresentation(
            Icons.Rounded.VideoCall,
            R.string.parental_category_video_chat_title,
            R.string.parental_category_video_chat_desc,
            R.string.parental_category_video_chat_guide,
        )

        ParentalRiskCategory.ANONYMOUS_MESSAGING -> CategoryPresentation(
            Icons.Rounded.QuestionAnswer,
            R.string.parental_category_anonymous_title,
            R.string.parental_category_anonymous_desc,
            R.string.parental_category_anonymous_guide,
        )

        ParentalRiskCategory.LIVE_STREAMING -> CategoryPresentation(
            Icons.Rounded.LiveTv,
            R.string.parental_category_streaming_title,
            R.string.parental_category_streaming_desc,
            R.string.parental_category_streaming_guide,
        )

        ParentalRiskCategory.DATING -> CategoryPresentation(
            Icons.Rounded.Favorite,
            R.string.parental_category_dating_title,
            R.string.parental_category_dating_desc,
            R.string.parental_category_dating_guide,
        )

        ParentalRiskCategory.AI_COMPANIONS -> CategoryPresentation(
            Icons.Rounded.SmartToy,
            R.string.parental_category_ai_title,
            R.string.parental_category_ai_desc,
            R.string.parental_category_ai_guide,
        )

        ParentalRiskCategory.HIDDEN_VAULTS -> CategoryPresentation(
            Icons.Rounded.Lock,
            R.string.parental_category_vault_title,
            R.string.parental_category_vault_desc,
            R.string.parental_category_vault_guide,
        )

        ParentalRiskCategory.GAMBLING -> CategoryPresentation(
            Icons.Rounded.Casino,
            R.string.parental_category_gambling_title,
            R.string.parental_category_gambling_desc,
            R.string.parental_category_gambling_guide,
        )

        ParentalRiskCategory.FILTER_BYPASS -> CategoryPresentation(
            Icons.Rounded.VpnLock,
            R.string.parental_category_bypass_title,
            R.string.parental_category_bypass_desc,
            R.string.parental_category_bypass_guide,
        )
    }

internal val ParentalConcern.presentation: ConcernPresentation
    get() = when (this) {
        ParentalConcern.GROOMING_REPORTS -> ConcernPresentation(
            Icons.Rounded.ReportProblem,
            R.string.parental_concern_grooming_title,
            R.string.parental_concern_grooming_desc,
        )

        ParentalConcern.SEXTORTION_REPORTS -> ConcernPresentation(
            Icons.Rounded.Report,
            R.string.parental_concern_sextortion_title,
            R.string.parental_concern_sextortion_desc,
        )

        ParentalConcern.SAFETY_ACTION -> ConcernPresentation(
            Icons.Rounded.Gavel,
            R.string.parental_concern_safety_action_title,
            R.string.parental_concern_safety_action_desc,
        )

        ParentalConcern.ADULT_CONTENT -> ConcernPresentation(
            Icons.Rounded.NoAdultContent,
            R.string.parental_concern_adult_title,
            R.string.parental_concern_adult_desc,
        )

        ParentalConcern.STRANGER_CONTACT -> ConcernPresentation(
            Icons.Rounded.PersonAdd,
            R.string.parental_concern_strangers_title,
            R.string.parental_concern_strangers_desc,
        )

        ParentalConcern.LIVE_VIDEO -> ConcernPresentation(
            Icons.Rounded.Videocam,
            R.string.parental_concern_live_video_title,
            R.string.parental_concern_live_video_desc,
        )

        ParentalConcern.ANONYMITY -> ConcernPresentation(
            Icons.Rounded.Masks,
            R.string.parental_concern_anonymity_title,
            R.string.parental_concern_anonymity_desc,
        )

        ParentalConcern.DISAPPEARING_MESSAGES -> ConcernPresentation(
            Icons.Rounded.Timelapse,
            R.string.parental_concern_disappearing_title,
            R.string.parental_concern_disappearing_desc,
        )

        ParentalConcern.LOCATION_SHARING -> ConcernPresentation(
            Icons.Rounded.LocationOn,
            R.string.parental_concern_location_title,
            R.string.parental_concern_location_desc,
        )

        ParentalConcern.BULLYING -> ConcernPresentation(
            Icons.Rounded.SentimentVeryDissatisfied,
            R.string.parental_concern_bullying_title,
            R.string.parental_concern_bullying_desc,
        )

        ParentalConcern.AI_ROLE_PLAY -> ConcernPresentation(
            Icons.Rounded.SmartToy,
            R.string.parental_concern_ai_title,
            R.string.parental_concern_ai_desc,
        )

        ParentalConcern.HIDES_CONTENT -> ConcernPresentation(
            Icons.Rounded.VisibilityOff,
            R.string.parental_concern_hides_title,
            R.string.parental_concern_hides_desc,
        )

        ParentalConcern.BYPASSES_CONTROLS -> ConcernPresentation(
            Icons.Rounded.LockOpen,
            R.string.parental_concern_bypass_title,
            R.string.parental_concern_bypass_desc,
        )

        ParentalConcern.GAMBLING -> ConcernPresentation(
            Icons.Rounded.Casino,
            R.string.parental_concern_gambling_title,
            R.string.parental_concern_gambling_desc,
        )

        ParentalConcern.VIOLENT_CONTENT -> ConcernPresentation(
            Icons.Rounded.Dangerous,
            R.string.parental_concern_violence_title,
            R.string.parental_concern_violence_desc,
        )

        ParentalConcern.SPENDING -> ConcernPresentation(
            Icons.Rounded.Payments,
            R.string.parental_concern_spending_title,
            R.string.parental_concern_spending_desc,
        )

        ParentalConcern.COMPULSIVE_USE -> ConcernPresentation(
            Icons.Rounded.AllInclusive,
            R.string.parental_concern_compulsive_title,
            R.string.parental_concern_compulsive_desc,
        )
    }
