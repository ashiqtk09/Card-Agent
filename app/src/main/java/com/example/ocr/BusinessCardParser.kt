package com.example.ocr

object BusinessCardParser {

    private const val LOW_CONFIDENCE_THRESHOLD = 0.78f

    private val EMAIL_REGEX = Regex(
        "\\b([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b",
    )

    private val WEBSITE_REGEX = Regex(
        "(?i)\\b((?:https?://)?(?:www\\.)?[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+(?:/[^\\s,;]*)?)",
    )

    private val PHONE_CANDIDATE_REGEX = Regex(
        "(?:\\+|00)?\\d[\\d\\s().-]{6,18}\\d",
    )

    private val FAX_PREFIX_REGEX = Regex("(?i)^(?:f|fax)\\s*[:.\\-]?\\s*")
    private val MOBILE_LABEL_REGEX = Regex("(?i)(?:^|\\b)(?:m|mob|mobile|cell|c|whatsapp|wa)\\s*[:.\\-]?\\s*")
    private val TEL_LABEL_REGEX = Regex("(?i)(?:^|\\b)(?:t|tel|telephone|ph|phone|o|office|d|direct|p)\\s*[:.\\-]?\\s*")
    private val EMAIL_LABEL_REGEX = Regex("(?i)^(?:e|email|e-mail|mail)\\s*[:.\\-]?\\s*")
    private val WEB_LABEL_REGEX = Regex("(?i)^(?:w|web|website|url|site)\\s*[:.\\-]?\\s*")

    private val FREE_EMAIL_DOMAINS = setOf(
        "gmail", "yahoo", "hotmail", "outlook", "icloud", "live", "msn",
        "protonmail", "proton", "aol", "mail", "ymail", "me", "example",
    )

    private val VALID_TLDS = setOf(
        "com", "org", "net", "io", "co", "ai", "app", "dev", "tech", "biz",
        "info", "edu", "gov", "me", "us", "uk", "ca", "au", "de", "fr", "es",
        "it", "nl", "ch", "se", "no", "ae", "om", "sa", "qa", "kw", "bh",
        "in", "sg", "jp", "cn", "kr", "br", "mx", "za", "nz", "eu", "global",
        "agency", "studio", "design", "digital", "capital", "advisory", "group",
        "partners", "solutions", "consulting", "ventures", "media", "cloud",
        "health", "finance", "law", "legal", "realty", "holdings",
    )

    private val DESIGNATION_KEYWORDS = listOf(
        "chief executive officer", "chief technology officer", "chief financial officer",
        "chief operating officer", "chief marketing officer", "chief information officer",
        "chief product officer", "chief revenue officer", "chief strategy officer",
        "vice president", "managing director", "managing partner", "senior partner",
        "executive director", "creative director", "strategy director", "marketing director",
        "sales director", "product director", "engineering director", "operations director",
        "art director", "technical director", "general manager", "product manager",
        "project manager", "account manager", "program manager", "engineering manager",
        "operations manager", "marketing manager", "sales manager", "branch manager",
        "business development", "head of", "co-founder", "cofounder", "founder",
        "ceo", "cto", "cfo", "coo", "cmo", "cio", "cpo", "cro", "svp", "evp", "vp",
        "president", "chairman", "chairwoman", "partner", "principal", "director",
        "lead", "manager", "architect", "engineer", "developer", "designer",
        "consultant", "advisor", "adviser", "analyst", "specialist", "coordinator",
        "executive", "officer", "associate", "attorney", "lawyer", "counsel",
        "accountant", "auditor", "broker", "agent", "representative", "professor",
        "lecturer", "researcher", "scientist", "physician", "surgeon", "dentist",
        "pharmacist", "therapist", "strategist", "producer", "editor", "writer",
        "journalist", "photographer", "supervisor", "administrator", "owner",
        "proprietor", "treasurer", "secretary", "president", "intern", "fellow",
    )

    private val COMPANY_SUFFIX_REGEX = Regex(
        "(?i)\\b(inc\\.?|llc|l\\.l\\.c\\.?|ltd\\.?|limited|corp\\.?|corporation|co\\.?|company|" +
            "group|holdings|partners|advisory|capital|ventures|labs|laboratory|laboratories|" +
            "tech|technologies|technology|solutions|systems|consulting|consultancy|studio|studios|" +
            "agency|associates|enterprises|industries|services|global|international|gmbh|ag|s\\.a\\.?|" +
            "pvt\\.?|plc|llp|bank|financial|insurance|clinic|hospital|medical|university|college|" +
            "institute|foundation|media|networks|digital|logistics|properties|realty|estates|" +
            "architects|builders|construction|manufacturing|energy|motors|aviation|pharma|foods)\\b",
    )

    private val ADDRESS_KEYWORD_REGEX = Regex(
        "(?i)\\b(st\\.?|street|ave\\.?|avenue|rd\\.?|road|blvd\\.?|boulevard|dr\\.?|drive|" +
            "ln\\.?|lane|way|ct\\.?|court|pl\\.?|place|plaza|sq\\.?|square|pkwy|parkway|" +
            "hwy|highway|tower|towers|floor|fl\\.?|suite|ste\\.?|unit|apt\\.?|bldg\\.?|building|" +
            "box|p\\.?o\\.?\\s*box|park|district|center|centre|mall|complex|block|sector|zone|" +
            "phase|industrial|estate|city|state|county|province|dubai|abu dhabi|muscat|riyadh|" +
            "doha|london|new york|san francisco|singapore|tokyo|toronto|sydney|mumbai|bengaluru|" +
            "bangalore|delhi|usa|u\\.s\\.a\\.?|uk|u\\.k\\.?|uae|u\\.a\\.e\\.?|oman|qatar|india|" +
            "canada|australia|germany|france|spain)\\b",
    )

    private val POSTAL_CODE_REGEX = Regex(
        "(?i)\\b(?:\\d{5}(?:-\\d{4})?|[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2}|[A-Z]\\d[A-Z]\\s*\\d[A-Z]\\d|\\d{6})\\b",
    )

    private val STREET_NUMBER_REGEX = Regex("^\\d{1,5}[A-Za-z]?\\s+[A-Za-z].+")

    private val NAME_PREFIX_REGEX = Regex("(?i)^(?:dr|mr|mrs|ms|miss|prof|eng)\\.?\\s+")

    fun parse(lines: List<RecognizedLine>): ExtractedCardData {
        val cleanedLines = lines
            .map { it.copy(text = it.text.replace(Regex("\\s+"), " ").trim()) }
            .filter { it.text.isNotBlank() }

        if (cleanedLines.isEmpty()) {
            return ExtractedCardData(
                ocrFailed = true,
                failureReason = "No text detected on business card.",
            )
        }

        val rawText = cleanedLines.joinToString("\n") { it.text }
        val uncertain = mutableSetOf<CardField>()

        // Expand pipe / bullet separated segments on single lines for contact info while preserving layout metadata
        val segments = cleanedLines.flatMap { line ->
            val parts = line.text.split(Regex("\\s*[|•·]\\s*")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size > 1) {
                parts.map { part -> line.copy(text = part) }
            } else {
                listOf(line)
            }
        }

        val consumedIndices = mutableSetOf<Int>()

        // 1. Extract Email
        var email = ""
        var emailConfidence: Float? = null
        for ((index, seg) in segments.withIndex()) {
            val match = EMAIL_REGEX.find(seg.text)
            if (match != null) {
                email = match.groupValues[1].trim().trimEnd('.', ',', ';')
                emailConfidence = seg.confidence
                val stripped = EMAIL_LABEL_REGEX.replace(seg.text.replace(match.value, ""), "").trim()
                if (stripped.length <= 2) {
                    consumedIndices.add(index)
                }
                break
            }
        }
        if (email.isNotBlank()) {
            if (!ContactValidator.isValidEmail(email) || isLowConfidence(emailConfidence)) {
                uncertain.add(CardField.EMAIL)
            }
        }

        // 2. Extract Company Website (excluding emails)
        var website = ""
        var websiteConfidence: Float? = null
        for ((index, seg) in segments.withIndex()) {
            val withoutEmail = EMAIL_REGEX.replace(seg.text, " ")
            val match = WEBSITE_REGEX.find(withoutEmail)
            if (match != null) {
                val candidate = match.groupValues[1].trim().trimEnd('.', ',', ';', ')')
                if (looksLikeWebsite(candidate)) {
                    website = candidate
                    websiteConfidence = seg.confidence
                    val stripped = WEB_LABEL_REGEX.replace(withoutEmail.replace(match.value, ""), "").trim()
                    if (stripped.length <= 2) {
                        consumedIndices.add(index)
                    }
                    break
                }
            }
        }
        if (website.isNotBlank()) {
            if (!ContactValidator.isValidWebsite(website) || isLowConfidence(websiteConfidence)) {
                uncertain.add(CardField.WEBSITE)
            }
        }

        // 3. Extract Mobile Number (preserving country code, prioritizing Mobile labels, skipping Fax)
        data class PhoneMatch(
            val value: String,
            val score: Int,
            val index: Int,
            val confidence: Float?,
            val hasExplicitMobileLabel: Boolean,
        )

        val phoneMatches = mutableListOf<PhoneMatch>()
        for ((index, seg) in segments.withIndex()) {
            if (index in consumedIndices) continue
            val text = seg.text
            if (FAX_PREFIX_REGEX.containsMatchIn(text)) {
                consumedIndices.add(index)
                continue
            }
            val matches = PHONE_CANDIDATE_REGEX.findAll(text).toList()
            for (m in matches) {
                val rawPhone = m.value.trim().trimEnd('-', '.', ')')
                val digitCount = rawPhone.count(Char::isDigit)
                if (digitCount in 7..15) {
                    val beforeMatch = text.substring(0, m.range.first)
                    val isFaxInline = Regex("(?i)\\b(?:f|fax)\\s*[:.\\-]?\\s*$").containsMatchIn(beforeMatch)
                    if (isFaxInline) continue

                    val isMobileLabeled = MOBILE_LABEL_REGEX.containsMatchIn(beforeMatch) ||
                        MOBILE_LABEL_REGEX.containsMatchIn(text)
                    val isTelLabeled = TEL_LABEL_REGEX.containsMatchIn(beforeMatch) ||
                        TEL_LABEL_REGEX.containsMatchIn(text)
                    val hasCountryCode = rawPhone.startsWith("+") || rawPhone.startsWith("00")

                    var score = 10
                    if (isMobileLabeled) score += 30
                    if (hasCountryCode) score += 12
                    if (isTelLabeled && !isMobileLabeled) score += 4
                    if (digitCount in 9..13) score += 5

                    phoneMatches.add(
                        PhoneMatch(
                            value = normalizePhoneDisplay(rawPhone),
                            score = score,
                            index = index,
                            confidence = seg.confidence,
                            hasExplicitMobileLabel = isMobileLabeled,
                        ),
                    )
                }
            }
        }

        var mobileNumber = ""
        if (phoneMatches.isNotEmpty()) {
            val sortedPhones = phoneMatches.sortedByDescending { it.score }
            val bestPhone = sortedPhones.first()
            mobileNumber = bestPhone.value
            val segText = segments[bestPhone.index].text
            val remainder = PHONE_CANDIDATE_REGEX.replace(segText, "")
                .let { MOBILE_LABEL_REGEX.replace(it, "") }
                .let { TEL_LABEL_REGEX.replace(it, "") }
                .trim(' ', ':', '-', '/', ',', '.')
            if (remainder.length <= 2) {
                consumedIndices.add(bestPhone.index)
            }
            val competingUnlabeledPhones = sortedPhones.count { !it.hasExplicitMobileLabel } >= 2 &&
                !bestPhone.hasExplicitMobileLabel
            val digitCount = mobileNumber.count(Char::isDigit)
            if (isLowConfidence(bestPhone.confidence) ||
                digitCount !in 8..15 ||
                competingUnlabeledPhones
            ) {
                uncertain.add(CardField.MOBILE)
            }
        }

        // 4. Extract Business Address lines
        val addressIndices = mutableListOf<Int>()
        for ((index, seg) in segments.withIndex()) {
            if (index in consumedIndices) continue
            if (looksLikeAddressLine(seg.text)) {
                addressIndices.add(index)
            }
        }
        val address = addressIndices.joinToString(", ") { segments[it].text.trimEnd(',') }
        if (addressIndices.isNotEmpty()) {
            consumedIndices.addAll(addressIndices)
            if (addressIndices.any { isLowConfidence(segments[it].confidence) }) {
                uncertain.add(CardField.ADDRESS)
            }
        }

        // 5. Remaining lines -> Designation, Company Name, Full Name
        val remaining = segments.mapIndexedNotNull { idx, seg ->
            if (idx in consumedIndices) null else IndexedValue(idx, seg)
        }

        val emailDomainCore = extractDomainCore(email)
        val websiteDomainCore = extractWebsiteCore(website)
        val brandHints = listOfNotNull(emailDomainCore, websiteDomainCore).distinct()
        val emailLocalPart = email.substringBefore("@", "").lowercase()

        var designation = ""
        var designationIndex: Int? = null
        var designationConfidence: Float? = null

        var companyName = ""
        var companyIndex: Int? = null
        var companyConfidence: Float? = null

        var name = ""
        var nameIndex: Int? = null
        var nameConfidence: Float? = null

        // 5a. Check for combined "Designation, Company" or "Designation at Company" line if needed
        for ((idx, seg) in remaining) {
            val text = seg.text
            if (matchesDesignation(text)) {
                val commaParts = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (commaParts.size == 2 &&
                    matchesDesignation(commaParts[0]) &&
                    !matchesDesignation(commaParts[1]) &&
                    (COMPANY_SUFFIX_REGEX.containsMatchIn(commaParts[1]) ||
                        matchesBrandHint(commaParts[1], brandHints))
                ) {
                    designation = commaParts[0]
                    companyName = commaParts[1]
                    designationIndex = idx
                    companyIndex = idx
                    designationConfidence = seg.confidence
                    companyConfidence = seg.confidence
                    break
                } else {
                    designation = text
                    designationIndex = idx
                    designationConfidence = seg.confidence
                    break
                }
            }
        }

        // 5b. Identify Company Name among remaining lines (not yet taken as designation)
        if (companyName.isBlank()) {
            val companyCandidates = remaining.filter { it.index != designationIndex }
            val scoredCompanies = companyCandidates.map { (idx, seg) ->
                var score = 0
                val text = seg.text
                if (COMPANY_SUFFIX_REGEX.containsMatchIn(text)) score += 35
                if (matchesBrandHint(text, brandHints)) score += 40
                if (text == text.uppercase() && text.length >= 3 && text.any(Char::isLetter)) score += 10
                if (text.contains("&")) score += 8
                if (looksLikePersonName(text) && !COMPANY_SUFFIX_REGEX.containsMatchIn(text)) score -= 12
                Triple(idx, seg, score)
            }.filter { it.third > 0 }.sortedByDescending { it.third }

            if (scoredCompanies.isNotEmpty()) {
                val best = scoredCompanies.first()
                companyName = best.second.text
                companyIndex = best.first
                companyConfidence = best.second.confidence
            }
        }

        // 5c. Identify Full Name among remaining lines
        val nameCandidates = remaining.filter {
            it.index != designationIndex && it.index != companyIndex
        }

        if (nameCandidates.isNotEmpty()) {
            val maxHeight = nameCandidates.maxOf { it.value.heightPx }.coerceAtLeast(1)
            val scoredNames = nameCandidates.map { (idx, seg) ->
                var score = 0
                val text = seg.text
                if (looksLikePersonName(text)) score += 30
                if (emailLocalPart.length >= 3 && matchesEmailLocalPart(text, emailLocalPart)) score += 35
                // Visual prominence: larger font height and higher position on the card favor person name
                if (seg.heightPx > 0 && seg.heightPx >= (maxHeight * 0.85f)) score += 15
                if (idx == remaining.firstOrNull()?.index) score += 8
                if (designationIndex != null && idx < designationIndex) score += 10
                if (text.any(Char::isDigit)) score -= 40
                if (COMPANY_SUFFIX_REGEX.containsMatchIn(text)) score -= 25
                Triple(idx, seg, score)
            }.sortedByDescending { it.third }

            val bestName = scoredNames.first()
            if (bestName.third > 0) {
                name = bestName.second.text
                nameIndex = bestName.first
                nameConfidence = bestName.second.confidence
                if (bestName.third < 30) {
                    uncertain.add(CardField.NAME)
                }
            }
        }

        // 5d. Fallback: if Company is still blank and there is another unconsumed non-noise line
        if (companyName.isBlank()) {
            val leftover = remaining.filter {
                it.index != designationIndex && it.index != nameIndex &&
                    it.value.text.length >= 2 &&
                    !it.value.text.all { ch -> !ch.isLetter() }
            }
            if (leftover.isNotEmpty()) {
                val candidate = leftover.first()
                companyName = candidate.value.text
                companyConfidence = candidate.value.confidence
                // Flag as uncertain since it didn't match an explicit corporate suffix or domain hint
                if (!matchesBrandHint(companyName, brandHints)) {
                    uncertain.add(CardField.COMPANY)
                }
            }
        }

        // 5e. Fallback: if Designation is still blank and a short line sits right after Name
        if (designation.isBlank() && nameIndex != null) {
            val afterName = remaining.firstOrNull {
                it.index > nameIndex &&
                    it.index != companyIndex &&
                    it.value.text.length in 3..48 &&
                    !it.value.text.any(Char::isDigit)
            }
            if (afterName != null) {
                designation = afterName.value.text
                designationConfidence = afterName.value.confidence
                uncertain.add(CardField.DESIGNATION)
            }
        }

        // Low OCR confidence checks
        if (name.isNotBlank() && isLowConfidence(nameConfidence)) uncertain.add(CardField.NAME)
        if (designation.isNotBlank() && isLowConfidence(designationConfidence)) uncertain.add(CardField.DESIGNATION)
        if (companyName.isNotBlank() && isLowConfidence(companyConfidence)) uncertain.add(CardField.COMPANY)

        val result = ExtractedCardData(
            name = name.trim(),
            mobileNumber = mobileNumber.trim(),
            designation = designation.trim(),
            companyName = companyName.trim(),
            companyWebsite = website.trim(),
            address = address.trim(),
            email = email.trim(),
            uncertainFields = uncertain,
            rawText = rawText,
            ocrFailed = false,
        )

        return if (result.hasAnyExtractedField) {
            result
        } else {
            result.copy(
                ocrFailed = true,
                failureReason = "Detected text could not be matched to contact fields.",
            )
        }
    }

    private fun isLowConfidence(confidence: Float?): Boolean {
        return confidence != null && confidence > 0f && confidence < LOW_CONFIDENCE_THRESHOLD
    }

    private fun normalizePhoneDisplay(raw: String): String {
        val compact = raw.replace(Regex("\\s+"), " ").trim()
        return if (compact.startsWith("00") && compact.count(Char::isDigit) > 9) {
            "+${compact.drop(2)}"
        } else {
            compact
        }
    }

    private fun looksLikeWebsite(candidate: String): Boolean {
        if (candidate.contains("@")) return false
        val lower = candidate.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) {
            return true
        }
        val host = lower.substringBefore("/")
        val tld = host.substringAfterLast(".", "")
        return tld in VALID_TLDS && host.substringBeforeLast(".").length >= 2
    }

    private fun looksLikeAddressLine(text: String): Boolean {
        if (text.length < 5) return false
        val hasKeyword = ADDRESS_KEYWORD_REGEX.containsMatchIn(text)
        val hasPostal = POSTAL_CODE_REGEX.containsMatchIn(text)
        val hasStreetNum = STREET_NUMBER_REGEX.matches(text)
        val hasCommaCityPattern = text.contains(",") && text.split(",").size >= 2 &&
            (hasKeyword || hasPostal || text.any(Char::isDigit))
        return hasStreetNum || hasPostal || hasCommaCityPattern || (hasKeyword && text.length >= 8)
    }

    private fun matchesDesignation(text: String): Boolean {
        val lower = text.lowercase()
        if (text.length > 65 || text.count(Char::isDigit) > 2) return false
        return DESIGNATION_KEYWORDS.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(lower)
        }
    }

    private fun looksLikePersonName(text: String): Boolean {
        val stripped = NAME_PREFIX_REGEX.replace(text.trim(), "")
        if (stripped.length !in 3..42) return false
        if (stripped.any(Char::isDigit)) return false
        if (stripped.contains("@") || stripped.contains("/") || stripped.contains(":")) return false
        val words = stripped.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size !in 2..4) return false
        val validWordRegex = Regex("^[A-Za-z][A-Za-z.'’-]*$")
        return words.all { validWordRegex.matches(it) }
    }

    private fun extractDomainCore(email: String): String? {
        if (!email.contains("@")) return null
        val domain = email.substringAfter("@").lowercase().substringBefore(".")
        return if (domain.length >= 3 && domain !in FREE_EMAIL_DOMAINS) domain else null
    }

    private fun extractWebsiteCore(website: String): String? {
        if (website.isBlank()) return null
        val host = website.lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore(".")
        return if (host.length >= 3 && host !in FREE_EMAIL_DOMAINS) host else null
    }

    private fun matchesBrandHint(text: String, brandHints: List<String>): Boolean {
        if (brandHints.isEmpty()) return false
        val normalized = text.lowercase().replace(Regex("[^a-z0-9]"), "")
        return brandHints.any { hint ->
            normalized.contains(hint) || hint.contains(normalized).and(normalized.length >= 4)
        }
    }

    private fun matchesEmailLocalPart(candidateName: String, emailLocalPart: String): Boolean {
        val tokens = candidateName.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.length >= 3 }
        return tokens.any { token -> emailLocalPart.contains(token) }
    }
}
