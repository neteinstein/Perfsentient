package org.neteinstein.perfsentient.core.domain.scam

/** A name scammers give fake apps, and the packages that really carry it. */
data class ImpersonatedName(
    val name: String,
    /**
     * Words that, alone or together, make an app label look like [name]. A keyword starting with
     * "=" must be the whole label (for everyday words: "=settings" flags "Settings", not "App settings").
     */
    val keywords: List<String>,
    /** The real apps' packages: never flagged for using the name. */
    val genuinePackages: Set<String>,
)

/**
 * Bundled lists behind the scam and fraud detectors (no network). Every entry has a source in
 * docs/scam-signals-research.md.
 */
class ScamSignalCatalog(
    /** Chat, social and email apps: an APK opened from one of these came from a link someone sent. */
    val messagingApps: Set<String> = MESSAGING_APPS,
    /** Browsers and file managers: an APK opened from these was downloaded or copied by hand. */
    val browsersAndFiles: Set<String> = BROWSERS_AND_FILES,
    /** Remote-access apps that scammers ask victims to install. */
    val remoteAccessApps: Set<String> = REMOTE_ACCESS_APPS,
    val impersonatedNames: List<ImpersonatedName> = IMPERSONATED_NAMES,
) {
    private val matchers = impersonatedNames.map { entry ->
        entry to entry.keywords.map { keyword ->
            Keyword(LabelSkeleton.tokens(keyword.removePrefix(EXACT)), exact = keyword.startsWith(EXACT))
        }
    }

    fun isMessagingApp(pkg: String?): Boolean = pkg != null && pkg in messagingApps

    fun isBrowserOrFiles(pkg: String?): Boolean = pkg != null && pkg in browsersAndFiles

    fun isRemoteAccessApp(pkg: String): Boolean = pkg in remoteAccessApps

    /** The well-known name [label] imitates, if any, ignoring case, accents, spacing and look-alike characters. */
    fun impersonatedBy(label: String): ImpersonatedName? {
        val tokens = LabelSkeleton.tokens(label)
        if (tokens.isEmpty()) return null
        val compact = tokens.joinToString("")
        return matchers.firstOrNull { (_, keywords) ->
            keywords.any { keyword -> keyword.tokens.isNotEmpty() && keyword.matches(tokens, compact) }
        }?.first
    }

    private data class Keyword(val tokens: List<String>, val exact: Boolean) {
        private val joined = tokens.joinToString("")

        fun matches(label: List<String>, compact: String): Boolean = when {
            compact == joined -> true

            exact -> false

            label.windowed(tokens.size).any { it == tokens } -> true

            // "GBWhatsApp", "WhatsAppPlus": long brand names glued to a prefix or suffix.
            else -> joined.length >= MIN_GLUED_LENGTH && joined in compact
        }
    }

    companion object {
        private const val EXACT = "="
        private const val MIN_GLUED_LENGTH = 6

        val MESSAGING_APPS = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",
            "org.thoughtcrime.securesms",
            "com.facebook.orca",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.facebook.mlite",
            "com.instagram.android",
            "com.viber.voip",
            "com.tencent.mm",
            "jp.naver.line.android",
            "com.kakao.talk",
            "com.imo.android.imoim",
            "com.discord",
            "com.snapchat.android",
            "com.zhiliaoapp.musically",
            "com.skype.raider",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.samsung.android.email.provider",
            "com.yahoo.mobile.client.android.mail",
            "ch.protonmail.android",
        )

        val BROWSERS_AND_FILES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.focus",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.opera.gx",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.kiwibrowser.browser",
            "com.UCMobile.intl",
            "com.mi.globalbrowser",
            "com.huawei.browser",
            "com.yandex.browser",
            "com.google.android.apps.nbu.files",
            "com.google.android.documentsui",
            "com.android.documentsui",
            "com.sec.android.app.myfiles",
            "com.mi.android.globalFileexplorer",
        )

        val REMOTE_ACCESS_APPS = setOf(
            "com.anydesk.anydeskandroid",
            "com.teamviewer.quicksupport.market",
            "com.teamviewer.host.market",
            "com.carriez.flutter_hbb",
            "com.sand.airdroid",
            "com.splashtop.sos",
            "com.zoho.assist.agent",
        )

        private fun name(name: String, vararg keywords: String, genuine: Set<String> = emptySet()) =
            ImpersonatedName(name, keywords.toList(), genuine)

        val IMPERSONATED_NAMES = listOf(
            // System components: a sideloaded app never legitimately carries these names.
            name("System update", "system update", "software update", "security update", "android update", "firmware update"),
            name("Google Play services", "play services", "google services", "google play services"),
            name("Google Play Protect", "play protect"),
            name("Google Play Store", "play store", "google play"),
            name("Android System", "android system", "system service", "=system", "=system ui"),
            name("Settings", "=settings"),
            name(
                "Chrome",
                "=chrome",
                "chrome update",
                "chrome browser",
                genuine = setOf("com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary"),
            ),
            // FluBot and its successors posed as parcel tracking and voicemail.
            name("Parcel tracking", "dhl", "fedex", "ups tracking", "parcel tracking", "track parcel", "package tracking"),
            name("Voicemail", "voicemail", "voice mail"),
            name("Flash Player", "flash player", "adobe flash"),
            // Messaging apps: modded copies ("GB", "Plus") are a common malware carrier.
            name("WhatsApp", "whatsapp", genuine = setOf("com.whatsapp", "com.whatsapp.w4b")),
            name(
                "Telegram",
                "telegram",
                genuine = setOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"),
            ),
            name("Signal", "=signal", "signal messenger", "signal private messenger", genuine = setOf("org.thoughtcrime.securesms")),
            name("Instagram", "instagram", genuine = setOf("com.instagram.android", "com.instagram.lite")),
            name(
                "Facebook",
                "facebook",
                genuine = setOf("com.facebook.katana", "com.facebook.lite", "com.facebook.orca", "com.facebook.mlite"),
            ),
            name(
                "TikTok",
                "tiktok",
                genuine = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"),
            ),
            name("Netflix", "netflix", genuine = setOf("com.netflix.mediaclient")),
            name("Spotify", "spotify", genuine = setOf("com.spotify.music", "com.spotify.lite")),
            // Payments, banks and crypto from Scam-Detector's impersonated-brand list.
            name("PayPal", "paypal", genuine = setOf("com.paypal.android.p2pmobile", "com.paypal.merchant.client")),
            name("Revolut", "revolut", genuine = setOf("com.revolut.revolut", "com.revolut.business")),
            name("Wise", "transferwise", genuine = setOf("com.transferwise.android")),
            name("N26", "=n26", "n26 bank", genuine = setOf("de.number26.android")),
            name("Cash App", "cash app", "cashapp", genuine = setOf("com.squareup.cash")),
            name("Venmo", "venmo", genuine = setOf("com.venmo")),
            name("MB WAY", "mb way", "mbway", genuine = setOf("pt.sibs.android.mbway")),
            name("Santander", "santander"),
            name("BBVA", "bbva"),
            name("CaixaBank", "caixabank"),
            name("Millennium BCP", "millennium bcp", "millenniumbcp"),
            name("Caixa Geral de Depósitos", "caixadirecta", "caixa geral"),
            name("Novo Banco", "novo banco", "novobanco"),
            name("ING", "ing bank", "ing banking"),
            name("Deutsche Bank", "deutsche bank"),
            name("Sparkasse", "sparkasse"),
            name("Barclays", "barclays"),
            name("HSBC", "hsbc"),
            name("Lloyds", "lloyds bank"),
            name("NatWest", "natwest"),
            name("Chase", "chase mobile", "chase bank"),
            name("Bank of America", "bank of america", "bofa"),
            name("Wells Fargo", "wells fargo", "wellsfargo"),
            name("Coinbase", "coinbase", genuine = setOf("com.coinbase.android")),
            name("Binance", "binance", genuine = setOf("com.binance.dev")),
            name("MetaMask", "metamask", genuine = setOf("io.metamask")),
            name("Trust Wallet", "trust wallet", "trustwallet", genuine = setOf("com.wallet.crypto.trustapp")),
            name("Ledger", "ledger live", genuine = setOf("com.ledger.live")),
            // Authenticators: a fake one is a straight path to one-time codes.
            // A fake of our partner app would read every text.
            name(
                "SMS Toolbox",
                "sms toolbox",
                "sms redirect",
                genuine = setOf("com.neteinstein.smsredirect", "com.neteinstein.smsredirect.test"),
            ),
            name("Google Authenticator", "google authenticator", genuine = setOf("com.google.android.apps.authenticator2")),
            name("Microsoft Authenticator", "microsoft authenticator", genuine = setOf("com.azure.authenticator")),
        )
    }
}
