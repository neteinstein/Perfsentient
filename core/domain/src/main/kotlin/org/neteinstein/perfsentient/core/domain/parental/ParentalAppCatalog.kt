package org.neteinstein.perfsentient.core.domain.parental

import org.neteinstein.perfsentient.core.model.ConcerningApp
import org.neteinstein.perfsentient.core.model.ParentalConcern
import org.neteinstein.perfsentient.core.model.ParentalConcern.ADULT_CONTENT
import org.neteinstein.perfsentient.core.model.ParentalConcern.AI_ROLE_PLAY
import org.neteinstein.perfsentient.core.model.ParentalConcern.ANONYMITY
import org.neteinstein.perfsentient.core.model.ParentalConcern.BULLYING
import org.neteinstein.perfsentient.core.model.ParentalConcern.BYPASSES_CONTROLS
import org.neteinstein.perfsentient.core.model.ParentalConcern.COMPULSIVE_USE
import org.neteinstein.perfsentient.core.model.ParentalConcern.DISAPPEARING_MESSAGES
import org.neteinstein.perfsentient.core.model.ParentalConcern.GAMBLING
import org.neteinstein.perfsentient.core.model.ParentalConcern.GROOMING_REPORTS
import org.neteinstein.perfsentient.core.model.ParentalConcern.HIDES_CONTENT
import org.neteinstein.perfsentient.core.model.ParentalConcern.LIVE_VIDEO
import org.neteinstein.perfsentient.core.model.ParentalConcern.LOCATION_SHARING
import org.neteinstein.perfsentient.core.model.ParentalConcern.SAFETY_ACTION
import org.neteinstein.perfsentient.core.model.ParentalConcern.SEXTORTION_REPORTS
import org.neteinstein.perfsentient.core.model.ParentalConcern.SPENDING
import org.neteinstein.perfsentient.core.model.ParentalConcern.STRANGER_CONTACT
import org.neteinstein.perfsentient.core.model.ParentalConcern.VIOLENT_CONTENT
import org.neteinstein.perfsentient.core.model.ParentalRiskCategory
import org.neteinstein.perfsentient.core.model.Severity
import org.neteinstein.perfsentient.core.model.Severity.HIGH
import org.neteinstein.perfsentient.core.model.Severity.LOW
import org.neteinstein.perfsentient.core.model.Severity.MEDIUM

/**
 * Apps that child-safety organisations, regulators and investigations warn parents about, keyed
 * by package name. Bundled with the app (no network): see docs/parental-help-research.md for the
 * sources behind every entry and its ages. Package names were checked against Google Play.
 */
class ParentalAppCatalog(val apps: List<ConcerningApp> = DEFAULT_APPS) {
    private val byPackage: Map<String, ConcerningApp> = buildMap {
        apps.forEach { app ->
            app.packageNames.forEach { pkg ->
                require(put(pkg, app) == null) { "Package $pkg is listed twice" }
            }
        }
    }

    /** Every package name in the catalogue. */
    val packageNames: Set<String> get() = byPackage.keys

    fun find(packageName: String): ConcerningApp? = byPackage[packageName]

    companion object {
        val DEFAULT_APPS: List<ConcerningApp> = buildCatalog {
            // Games where strangers can chat with players. Roblox: grooming lawsuits by US states
            // (2025-26) and a federal MDL; adults move children on to Discord / Snapchat.
            category(ParentalRiskCategory.GAMES_WITH_CHAT, recommendedAge = 13, severity = LOW, STRANGER_CONTACT, SPENDING) {
                app("roblox", "com.roblox.client", severity = MEDIUM, extra = setOf(GROOMING_REPORTS, SAFETY_ACTION))
                app("minecraft", "com.mojang.minecraftpe", recommendedAge = 10)
                app("fortnite", "com.epicgames.fortnite", extra = setOf(VIOLENT_CONTENT))
                app("free_fire", "com.dts.freefireth", "com.dts.freefiremax", extra = setOf(VIOLENT_CONTENT))
                app("pubg_mobile", "com.tencent.ig", extra = setOf(VIOLENT_CONTENT))
                app(
                    "cod_mobile",
                    "com.activision.callofduty.shooter",
                    recommendedAge = 17,
                    severity = MEDIUM,
                    extra = setOf(VIOLENT_CONTENT),
                )
                app(
                    "steam",
                    "com.valvesoftware.android.steam.community",
                    "com.valvesoftware.android.steam.friendsui",
                    minimumAge = 13,
                    extra = setOf(ADULT_CONTENT),
                )
            }

            // Avatar worlds and social VR: open chat, private rooms and sexual role-play (BBC / NSPCC on VRChat).
            category(ParentalRiskCategory.VIRTUAL_WORLDS, recommendedAge = 13, severity = MEDIUM, STRANGER_CONTACT, SPENDING) {
                app("avakin_life", "com.lockwoodpublishing.avakinlife", minimumAge = 13, extra = setOf(ADULT_CONTENT))
                app("imvu", "com.imvu.mobilecordova", minimumAge = 13, recommendedAge = 16, extra = setOf(ADULT_CONTENT))
                app("zepeto", "me.zepeto.main", severity = LOW)
                app("highrise", "com.pz.life.android", extra = setOf(ADULT_CONTENT))
                app(
                    "vrchat",
                    "com.vrchat.mobile.playstore",
                    minimumAge = 13,
                    recommendedAge = 16,
                    extra = setOf(GROOMING_REPORTS, ADULT_CONTENT),
                )
                app("rec_room", "com.recroomapp.app", severity = LOW)
            }

            // Mainstream social media. 16 follows Australia's Social Media Minimum Age law (Dec 2025).
            // Instagram and Snapchat are the top two sextortion platforms in NCRI's research.
            category(ParentalRiskCategory.SOCIAL_MEDIA, recommendedAge = 16, severity = MEDIUM, STRANGER_CONTACT, COMPULSIVE_USE) {
                app("instagram", "com.instagram.android", minimumAge = 13, extra = setOf(SEXTORTION_REPORTS, SAFETY_ACTION))
                app(
                    "tiktok",
                    "com.zhiliaoapp.musically",
                    "com.ss.android.ugc.trill",
                    "com.zhiliaoapp.musically.go",
                    minimumAge = 13,
                    extra = setOf(LIVE_VIDEO, SAFETY_ACTION),
                )
                app("facebook", "com.facebook.katana", minimumAge = 13, severity = LOW, extra = setOf(SAFETY_ACTION))
                app("threads", "com.instagram.barcelona", minimumAge = 13, severity = LOW)
                app("x", "com.twitter.android", minimumAge = 13, recommendedAge = 17, extra = setOf(ADULT_CONTENT))
                app("reddit", "com.reddit.frontpage", minimumAge = 13, recommendedAge = 17, extra = setOf(ADULT_CONTENT, ANONYMITY))
                app("tumblr", "com.tumblr", minimumAge = 13, recommendedAge = 17, extra = setOf(ADULT_CONTENT, ANONYMITY))
            }

            // Private, encrypted or disappearing chat: where groomers move children to.
            category(ParentalRiskCategory.PRIVATE_MESSAGING, recommendedAge = 16, severity = MEDIUM, STRANGER_CONTACT) {
                app(
                    "snapchat",
                    "com.snapchat.android",
                    minimumAge = 13,
                    extra = setOf(SEXTORTION_REPORTS, SAFETY_ACTION, DISAPPEARING_MESSAGES, LOCATION_SHARING),
                )
                app("discord", "com.discord", minimumAge = 13, extra = setOf(GROOMING_REPORTS, SAFETY_ACTION, ADULT_CONTENT))
                app(
                    "kik",
                    "kik.android",
                    minimumAge = 13,
                    recommendedAge = 17,
                    severity = HIGH,
                    extra = setOf(GROOMING_REPORTS, SAFETY_ACTION, ANONYMITY),
                )
                app(
                    "telegram",
                    "org.telegram.messenger",
                    "org.thunderdog.challegram",
                    recommendedAge = 17,
                    severity = HIGH,
                    extra = setOf(SAFETY_ACTION, ADULT_CONTENT, ANONYMITY),
                )
                // Everyday messengers: only flagged below their own minimum age.
                app("whatsapp", "com.whatsapp", minimumAge = 13, recommendedAge = 13, severity = LOW)
                app("messenger", "com.facebook.orca", minimumAge = 13, recommendedAge = 13, severity = LOW)
            }

            // "Tinder for teens": swipe to meet strangers nearby. Wizz was pulled from both stores in 2024 over sextortion.
            category(ParentalRiskCategory.FRIEND_FINDING, recommendedAge = 17, severity = MEDIUM, STRANGER_CONTACT, LOCATION_SHARING) {
                app(
                    "wizz",
                    "info.wizzapp",
                    minimumAge = 13,
                    recommendedAge = 18,
                    severity = HIGH,
                    extra = setOf(SEXTORTION_REPORTS, SAFETY_ACTION),
                )
                app("yubo", "co.yellw.yellowapp", minimumAge = 13, extra = setOf(LIVE_VIDEO))
                app("hoop", "com.dazz.hoop", minimumAge = 13)
                app("purp", "com.hubolabs.hubo", minimumAge = 13)
                app("befriend", "com.guppir.swipr", minimumAge = 13)
                app("wink", "co.ninecount.wink", "co.ninecount.soda", minimumAge = 13)
                app("lmk", "com.lightspace.lmk", minimumAge = 13, extra = setOf(ANONYMITY, SAFETY_ACTION))
            }

            // Random video chat with strangers (Omegle and its successors): adults only.
            category(
                ParentalRiskCategory.RANDOM_VIDEO_CHAT,
                recommendedAge = 18,
                severity = HIGH,
                LIVE_VIDEO,
                STRANGER_CONTACT,
                ADULT_CONTENT,
                ANONYMITY,
            ) {
                app("holla", "com.exutech.chacha")
                app("omega", "ly.omegle.android")
                app("camsea", "com.camsea.videochat")
                app("azar", "com.azarlive.android")
                app("chatous", "com.chatous.chatous")
                app("minichat", "mini.video.chat")
                app("ometv", "omegle.tv")
                app("chat_alternative", "com.chatroullete.alternative")
                app("chatspin", "com.Chatspin")
                app("emerald_chat", "com.cookie.emerald")
                app("monkey", "cool.monkey.android")
            }

            // Anonymous messages and Q&A: bullying. The FTC banned NGL from serving minors (2024) and sued Sendit (2025).
            category(ParentalRiskCategory.ANONYMOUS_MESSAGING, recommendedAge = 17, severity = MEDIUM, ANONYMITY, BULLYING) {
                app("ngl", "com.nglreactnative", recommendedAge = 18, severity = HIGH, extra = setOf(SAFETY_ACTION))
                app("sendit", "com.fullsenders.sendit", recommendedAge = 18, severity = HIGH, extra = setOf(SAFETY_ACTION))
                app("tellonym", "de.tellonym.app")
                app("askfm", "com.askfm", "app.askfm.mobile")
                app("whisper", "sh.whisper", recommendedAge = 18, severity = HIGH, extra = setOf(STRANGER_CONTACT, ADULT_CONTENT))
                app("jodel", "com.tellm.android.app", extra = setOf(LOCATION_SHARING))
            }

            // Live streaming with paid "gifts": gifts are a known grooming tactic (NYT 2024 on Bigo / Tango).
            category(ParentalRiskCategory.LIVE_STREAMING, recommendedAge = 18, severity = HIGH, LIVE_VIDEO, STRANGER_CONTACT, SPENDING) {
                app("bigo_live", "sg.bigo.live", extra = setOf(ADULT_CONTENT, SAFETY_ACTION))
                app("liveme", "com.cmcm.live", "com.plusme.live", extra = setOf(GROOMING_REPORTS))
                app("tango", "com.sgiggle.production", extra = setOf(ADULT_CONTENT))
                app("mico", "com.mico", recommendedAge = 17, extra = setOf(ADULT_CONTENT))
                app("younow", "younow.live", recommendedAge = 16, severity = MEDIUM)
                app("likee", "video.like", recommendedAge = 16, severity = MEDIUM)
                app("twitch", "tv.twitch.android.app", minimumAge = 13, recommendedAge = 16, severity = LOW)
                app("kick", "com.kick.mobile", minimumAge = 13, severity = MEDIUM, extra = setOf(GAMBLING, ADULT_CONTENT))
            }

            // Adult dating: 18+ by their own terms.
            category(
                ParentalRiskCategory.DATING,
                recommendedAge = 18,
                severity = HIGH,
                STRANGER_CONTACT,
                ADULT_CONTENT,
                LOCATION_SHARING,
            ) {
                listOf(
                    "tinder" to "com.tinder",
                    "bumble" to "com.bumble.app",
                    "hinge" to "co.hinge.app",
                    "grindr" to "com.grindrapp.android",
                    "badoo" to "com.badoo.mobile",
                    "pof" to "com.pof.android",
                    "okcupid" to "com.okcupid.okcupid",
                    "happn" to "com.ftw_and_co.happn",
                    "feeld" to "co.feeld",
                    "hily" to "com.hily.app",
                ).forEach { (id, pkg) -> app(id, pkg, minimumAge = 18) }
                listOf(
                    "tagged" to "com.taggedapp",
                    "meetme" to "com.myyearbook.m",
                    "skout" to "com.skout.android",
                ).forEach { (id, pkg) -> app(id, pkg, minimumAge = 18, extra = setOf(LIVE_VIDEO)) }
            }

            // AI companions: Common Sense Media / Stanford say no one under 18. Character.AI ended
            // open-ended chats for under-18s after teen-suicide lawsuits (2025).
            category(ParentalRiskCategory.AI_COMPANIONS, recommendedAge = 18, severity = HIGH, AI_ROLE_PLAY, ADULT_CONTENT) {
                app("character_ai", "ai.character.app", extra = setOf(SAFETY_ACTION))
                app("replika", "ai.replika.app", minimumAge = 18)
                app("chai", "com.Beauchamp.Messenger.external")
                app("talkie", "com.weaver.app.prod")
                app("polybuzz", "ai.socialapps.speakmaster")
                app("nomi", "ai.nomi.twa", minimumAge = 18)
                app("kindroid", "com.kindroid.app", minimumAge = 18)
            }

            // Disguised vaults (fake calculators etc.) that hide photos, chats or other apps.
            category(ParentalRiskCategory.HIDDEN_VAULTS, recommendedAge = 18, severity = MEDIUM, HIDES_CONTENT, BYPASSES_CONTROLS) {
                app("hide_it_pro", "com.hideitpro")
                app("calculator_photo_vault", "com.hld.anzenbokusucal")
                app("keepsafe", "com.kii.safe")
                app("lockmypix", "com.fourchars.lmpfree", "com.fourchars.lmp")
                app("gallery_vault", "com.thinkyeah.galleryvault")
                app("vaulty", "com.theronrogers.vaultyfree")
                app("photo_lock", "vault.gallery.lock")
                app("calculator_vault_app_hider", "com.app.calculator.vault.hider")
                app("hideu", "com.calculator.hideu")
            }

            // Betting and casino-style games.
            category(ParentalRiskCategory.GAMBLING, recommendedAge = 18, severity = MEDIUM, GAMBLING, SPENDING) {
                app("bet365", "com.bet365Wrapper.Bet365_Application", minimumAge = 18, severity = HIGH)
                app("draftkings", "com.draftkings.sportsbook", minimumAge = 18, severity = HIGH)
                app("fanduel", "com.fanduel.sportsbook", minimumAge = 18, severity = HIGH)
                app("slotomania", "air.com.playtika.slotomania")
                app("zynga_poker", "com.zynga.livepoker")
            }

            // Tools that get around filters and parental controls.
            category(ParentalRiskCategory.FILTER_BYPASS, recommendedAge = 16, severity = LOW, BYPASSES_CONTROLS) {
                app("tor_browser", "org.torproject.torbrowser", extra = setOf(ADULT_CONTENT))
                app("orbot", "org.torproject.android")
            }
        }
    }
}

private fun buildCatalog(block: CatalogBuilder.() -> Unit): List<ConcerningApp> = CatalogBuilder().apply(block).apps

private class CatalogBuilder {
    val apps = mutableListOf<ConcerningApp>()

    fun category(
        category: ParentalRiskCategory,
        recommendedAge: Int,
        severity: Severity,
        vararg concerns: ParentalConcern,
        block: CategoryScope.() -> Unit,
    ) {
        CategoryScope(category, recommendedAge, severity, concerns.toSet()).apply(block).apps.forEach(apps::add)
    }
}

private class CategoryScope(
    private val category: ParentalRiskCategory,
    private val defaultRecommendedAge: Int,
    private val defaultSeverity: Severity,
    private val defaultConcerns: Set<ParentalConcern>,
) {
    val apps = mutableListOf<ConcerningApp>()

    fun app(
        id: String,
        vararg packageNames: String,
        minimumAge: Int = 0,
        recommendedAge: Int = defaultRecommendedAge,
        severity: Severity = defaultSeverity,
        extra: Set<ParentalConcern> = emptySet(),
    ) {
        apps += ConcerningApp(
            id = id,
            packageNames = packageNames.toSet(),
            category = category,
            minimumAge = minimumAge,
            recommendedAge = recommendedAge,
            baseSeverity = severity,
            concerns = defaultConcerns + extra,
        )
    }
}
