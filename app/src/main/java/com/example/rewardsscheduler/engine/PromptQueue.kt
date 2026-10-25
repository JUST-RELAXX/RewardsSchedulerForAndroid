package com.example.rewardsscheduler.engine

/**
 * Prompt Queue and Offline Fallback Database.
 * Contains the 57 exact fallback queries extracted from the PC app.
 * Provides unique, non-repeating search queries for sessions.
 */
object PromptQueue {

    val FALLBACK_QUERIES: List<String> = listOf(
        // Trending / Everyday
        "best restaurants near me", "weather today", "latest news headlines",
        "how to make pasta", "top movies 2026", "best workout routines",
        "healthy breakfast ideas", "how to fix slow wifi", "best budget phones 2026",
        "funny cat videos", "how to learn guitar", "best hiking trails",
        "tips for better sleep", "home office setup ideas", "easy dinner recipes",
        "best books to read", "how to save money", "morning routine tips",
        "best travel destinations", "how to meditate", "best coding languages to learn",
        "how to grow indoor plants", "best productivity apps", "how to start a blog",
        "best netflix shows", "how to do laundry properly", "easy home workouts",
        "best podcasts 2026", "how to make smoothies", "best laptop for students",

        // Tech
        "artificial intelligence news", "best programming tutorials", "react vs angular",
        "python tips and tricks", "cloud computing basics", "cybersecurity tips",
        "machine learning for beginners", "web development trends", "best IDE for coding",
        "how does blockchain work", "best gaming monitors", "keyboard shortcuts windows",

        // Science & Knowledge
        "how do black holes work", "interesting space facts", "history of the internet",
        "how does DNA work", "renewable energy sources", "quantum computing explained",
        "evolution of humans", "deepest ocean trenches", "how vaccines work",
        "climate change solutions", "mars exploration updates", "photosynthesis explained",

        // Life & Culture
        "best music playlists", "how to cook rice perfectly", "yoga for beginners",
        "best board games", "how to tie a tie", "photography tips for beginners"
    )

    val DEFAULT_TOPICS: List<String> = listOf(
        "Air Quality Index", "Geopolitics News", "Satellite Traffic", "Gold Prices", "NFT Floor Prices", "AI Hardware Reviews",
        "VR Game Releases", "Streaming Trends", "Influencer Scandals", "Deepfake Detection", "Indie Games", "Mobile Esports",
        "Webtoons", "Light Novels", "Graphic Novels", "Audiobook Hits", "Vinyl Sales", "Immersive Theater", "Art Biennales",
        "Slow Travel", "Train Vacations", "Eco-Resorts", "Ultralight Packing", "Wild Camping", "Meal Prep Ideas",
        "Plant-Based Meat", "Michelin Guide", "Specialty Tea", "Zero-Proof Spirits", "Biohacking Tips", "Somatic Yoga",
        "Intermittent Fasting", "Digital Detox", "Sound Bathing", "Breathwork", "Upcycling Ideas", "Biophilic Design",
        "Smart Irrigation", "Aquascaping", "Cat Behavior", "Reptile Care", "Co-living Spaces", "Solar Incentives", "Gig Economy",
        "Remote Work Skills", "Passion Projects", "High-Yield Savings", "Crypto Tax Laws", "Labor Rights", "Voting Records",
        "Quantum Physics", "Mars Mission Updates", "Local Folklore", "Drone Photography", "Digital Art", "Sustainable Fabrics",
        "Vintage Revival", "Skincare Science", "Hydrogen Cars", "Autonomous Shuttles", "Electric Bikes", "Micro-Mobility"
    )

    /**
     * Builds a full queue of [totalNeeded] queries for a session.
     * Takes [fetchedPrompts] first (from index 0). If more queries are needed,
     * fills the remainder from shuffled [FALLBACK_QUERIES], never repeating a query
     * already used in the session.
     */
    fun buildSessionQueue(
        fetchedPrompts: List<String>,
        totalNeeded: Int
    ): List<String> {
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        // 1. Add fetched prompts first (prompt #1 at index 0)
        for (p in fetchedPrompts) {
            val trimmed = p.trim()
            if (trimmed.isNotEmpty() && seen.add(trimmed.lowercase())) {
                result.add(trimmed)
                if (result.size >= totalNeeded) return result
            }
        }

        // 2. Supplement from shuffled fallback queries if totalNeeded > fetched
        val shuffledFallbacks = FALLBACK_QUERIES.shuffled()
        for (q in shuffledFallbacks) {
            if (result.size >= totalNeeded) break
            if (seen.add(q.lowercase())) {
                result.add(q)
            }
        }

        // 3. If still needed, add with variation suffixes (same as PC app generateSearchQueue)
        val suffixes = listOf("tips", "guide", "tutorial", "explained", "2026", "best", "how to", "review")
        var suffixIdx = 0
        while (result.size < totalNeeded) {
            val base = FALLBACK_QUERIES.random()
            val queryWithSuffix = "$base ${suffixes[suffixIdx % suffixes.size]}"
            suffixIdx++
            if (seen.add(queryWithSuffix.lowercase())) {
                result.add(queryWithSuffix)
            }
        }

        return result
    }
}
