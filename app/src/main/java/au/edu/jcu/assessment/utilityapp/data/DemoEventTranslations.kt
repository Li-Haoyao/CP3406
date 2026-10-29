package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.model.EventTranslation

/** Authored translations for fictional local fixtures, never a substitute for remote feed content. */
object DemoEventTranslations {
    private const val NOTICE = "Demo event: brands, rewards and schedules are fictional and only illustrate the app."

    val byId = mapOf(
        "demo-tea" to EventTranslation(
            "A little starlight,\nin every sip.", "One limited cup. A whole new world.",
            listOf("Limited cup sleeve", "Character card", "Themed paper bag"),
            listOf("Check participating stores and serving times in your region.",
                "Order a collaboration drink or the specified set in store.",
                "Collect your cup sleeve and character card, then check the quantities."),
            "China / Malaysia / Singapore · Selected stores (demo)",
            "Purchase a specified collaboration drink (demo rules)", NOTICE),
        "demo-food" to EventTranslation(
            "Squad up.\nHotpot is waiting.", "Dinner with your favorite characters",
            listOf("Character badge", "Collaboration placemat"),
            listOf("Find a themed restaurant and check available reservation times.",
                "Show your reservation and order the specified set.",
                "Collect the collaboration gifts after your meal, following the event rules."),
            "Mainland China · Themed hotpot restaurants (demo)",
            "Specified set; some stores require a reservation (demo rules)", NOTICE),
        "demo-fashion" to EventTranslation(
            "Wear your\nnext adventure.", "A limited wardrobe for urban explorers",
            listOf("Printed hoodie", "Limited tote bag"),
            listOf("Confirm the release date and sales channels available in your region.",
                "Check sizes, materials and release details.",
                "Place your order when sales open, following the brand’s rules."),
            "Brand online store / Selected physical stores (demo)",
            "Purchase eligible items during the release period (demo rules)", NOTICE),
        "demo-retail" to EventTranslation(
            "Small surprises,\naround the corner.", "Collect a little everyday joy",
            listOf("Acrylic charm", "Limited stickers"),
            listOf("Check nearby participating stores and eligible items.",
                "Choose the product combination required by the event.",
                "Redeem your gifts at checkout according to the event rules."),
            "Malaysia / Singapore · Selected convenience stores (demo)",
            "Purchase the specified product combination (demo rules)", NOTICE),
        "demo-game" to EventTranslation(
            "Two worlds.\nOne adventure.", "A special mission across dimensions",
            listOf("Collaboration avatar", "Limited decoration"),
            listOf("Sign in to the game and open the collaboration event page.",
                "Check the missions and account level requirements.",
                "Complete the missions and claim your rewards on the event page."),
            "In game · Demo server", "Meet the required account level (demo rules)", NOTICE),
        "demo-coffee" to EventTranslation(
            "Let the afternoon\nslow down.", "A coffee break from another world",
            listOf("Themed coaster", "Postcard"),
            listOf("Check participating cafés and opening hours.",
                "Purchase the specified themed coffee.", "Collect your themed coaster and postcard."),
            "Malaysia / Singapore · Selected cafés (demo)", "Purchase a specified themed drink (demo rules)", NOTICE),
        "demo-ended" to EventTranslation(
            "Keep the good times\non this page.", "A record of a past collaboration",
            listOf("Bakery paper bag"),
            listOf("This fictional event has ended and demonstrates the past-event filter."),
            "Mainland China · Selected stores (demo)", "Ended; participation is closed", NOTICE)
    )
}
