package doublemoon.mahjongcraft.paper.i18n

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MessageServiceTest {
    private val messages = LocalizedMessages()

    @Test
    fun `plain returns english text for english locale`() {
        assertContains(messages.plain(Locale.ENGLISH, "command.action.ron"), "Ron")
    }

    @Test
    fun `plain returns chinese text for zh CN locale`() {
        val chinese = messages.plain(Locale.forLanguageTag("zh-CN"), "command.action.ron")
        assertTrue(chinese.isNotBlank())
        assertNotEquals("Ron", chinese)
    }

    @Test
    fun `normalize locale matches supported language variants`() {
        assertEquals(Locale.ENGLISH, messages.normalizeLocale("en-US"))
        assertEquals(Locale.forLanguageTag("zh-CN"), messages.normalizeLocale("zh-HK"))
        assertEquals(Locale.forLanguageTag("zh-CN"), messages.normalizeLocale("de-DE"))
    }

    @Test
    fun `render keeps command help text available`() {
        val rendered = messages.plain(Locale.ENGLISH, "command.help.create")
        assertContains(rendered, "/mahjong create")
        assertContains(rendered, "Create a new table")
    }
}
