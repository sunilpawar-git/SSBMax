package com.ssbmax.ui.marketplace

import com.ssbmax.core.domain.model.CoachingInstitute
import com.ssbmax.core.domain.model.InstituteType
import com.ssbmax.core.domain.model.PriceRange

/**
 * Mock data provider for SSB coaching institutes
 * This will be replaced with real API/Firestore data in production
 */
object MarketplaceMockData {

    fun getInstitutes(): List<CoachingInstitute> {
        return listOf(
            CoachingInstitute(
                id = "major_kalshi",
                name = "Major Kalshi Classes",
                description = "Premier SSB coaching institute with 30+ years of excellence. Specializes in comprehensive preparation for all SSB stages with experienced ex-defence personnel as mentors.",
                rating = 4.8f,
                reviewCount = 2450,
                location = "Sector 15, Chandigarh",
                city = "Chandigarh",
                state = "Chandigarh",
                type = InstituteType.BOTH,
                priceRange = PriceRange.PREMIUM,
                specializations = listOf(
                    "OIR & PPDT",
                    "Psychology Tests",
                    "GTO Tasks",
                    "Personal Interview"
                ),
                features = listOf(
                    "Mock SSB Tests",
                    "Personalized Feedback",
                    "Interview Practice",
                    "Group Discussion Sessions",
                    "Physical Fitness Training"
                ),
                phoneNumber = "+91-172-2700333",
                email = "info@majorkalshi.com",
                website = "www.majorkalshi.com",
                establishedYear = 1990,
                successRate = 85.5f,
                totalStudents = 15000
            ),
            CoachingInstitute(
                id = "cavalier",
                name = "Cavalier India",
                description = "Leading SSB coaching with focus on holistic personality development. Known for excellent GTO and psychology test preparation with modern teaching methodologies.",
                rating = 4.7f,
                reviewCount = 1890,
                location = "Defence Colony, New Delhi",
                city = "New Delhi",
                state = "Delhi",
                type = InstituteType.BOTH,
                priceRange = PriceRange.PREMIUM,
                specializations = listOf(
                    "GTO Excellence",
                    "Psychology Mastery",
                    "TAT & WAT",
                    "Conference Preparation"
                ),
                features = listOf(
                    "5-Day Mock SSB",
                    "Video Analysis",
                    "One-on-One Mentoring",
                    "Guest Lectures by Officers",
                    "Online Live Classes"
                ),
                phoneNumber = "+91-11-26333444",
                email = "contact@cavalierindia.com",
                website = "www.cavalierindia.com",
                establishedYear = 2005,
                successRate = 82.3f,
                totalStudents = 12000
            ),
            CoachingInstitute(
                id = "baalnoi",
                name = "Baalnoi Academy",
                description = "Specialized in OLQ development and personality enhancement. Focus on building confidence and leadership qualities essential for SSB selection.",
                rating = 4.6f,
                reviewCount = 1650,
                location = "Civil Lines, Allahabad",
                city = "Allahabad",
                state = "Uttar Pradesh",
                type = InstituteType.PHYSICAL,
                priceRange = PriceRange.MODERATE,
                specializations = listOf(
                    "OLQ Development",
                    "Personality Enhancement",
                    "Leadership Training",
                    "Communication Skills"
                ),
                features = listOf(
                    "Residential Coaching",
                    "Daily Mock Tests",
                    "Physical Training",
                    "Library & Study Material",
                    "Doubt Clearing Sessions"
                ),
                phoneNumber = "+91-532-2460555",
                email = "info@baalnoi.com",
                website = "www.baalnoi.com",
                establishedYear = 1998,
                successRate = 78.9f,
                totalStudents = 8500
            ),
            CoachingInstitute(
                id = "colonels_ssb",
                name = "Colonel's SSB Academy",
                description = "Founded by retired defence officers. Offers practical, experience-based training with focus on real SSB scenarios and authentic test simulations.",
                rating = 4.5f,
                reviewCount = 1420,
                location = "MG Road, Dehradun",
                city = "Dehradun",
                state = "Uttarakhand",
                type = InstituteType.PHYSICAL,
                priceRange = PriceRange.MODERATE,
                specializations = listOf(
                    "Mock SSB",
                    "Outdoor Training",
                    "Interview Techniques",
                    "PIQ Analysis"
                ),
                features = listOf(
                    "Ex-Defence Mentors",
                    "Outdoor GTO Setup",
                    "Small Batch Size",
                    "Personal Attention",
                    "Post-SSB Guidance"
                ),
                phoneNumber = "+91-135-2714888",
                email = "academy@colonelsssb.com",
                website = "www.colonelsssb.com",
                establishedYear = 2010,
                successRate = 76.5f,
                totalStudents = 6200
            ),
            CoachingInstitute(
                id = "olive_greens",
                name = "Olive Greens Institute",
                description = "Modern SSB coaching with digital learning tools and AI-powered performance analytics. Combines traditional methods with technology-driven insights.",
                rating = 4.4f,
                reviewCount = 980,
                location = "Connaught Place, New Delhi",
                city = "New Delhi",
                state = "Delhi",
                type = InstituteType.ONLINE,
                priceRange = PriceRange.BUDGET,
                specializations = listOf(
                    "Online Live Classes",
                    "AI-Powered Analysis",
                    "Digital Study Material",
                    "Recorded Sessions"
                ),
                features = listOf(
                    "24/7 Learning Access",
                    "Mobile App",
                    "Performance Dashboards",
                    "Unlimited Practice Tests",
                    "Community Forums"
                ),
                phoneNumber = "+91-11-41556677",
                email = "support@olivegreens.in",
                website = "www.olivegreens.in",
                establishedYear = 2018,
                successRate = 72.1f,
                totalStudents = 5000
            ),
            CoachingInstitute(
                id = "warriors_ssb",
                name = "Warriors SSB Academy",
                description = "Budget-friendly SSB coaching focusing on basics and fundamentals. Perfect for first-time aspirants looking for quality guidance at affordable prices.",
                rating = 4.3f,
                reviewCount = 750,
                location = "Kankarbagh, Patna",
                city = "Patna",
                state = "Bihar",
                type = InstituteType.BOTH,
                priceRange = PriceRange.BUDGET,
                specializations = listOf(
                    "Foundation Course",
                    "Basic OLQ Training",
                    "Test Familiarization",
                    "Group Practice"
                ),
                features = listOf(
                    "Affordable Fees",
                    "Weekend Batches",
                    "Flexible Timings",
                    "Study Material Included",
                    "Scholarship Program"
                ),
                phoneNumber = "+91-612-2234567",
                email = "info@warriorsssb.com",
                website = "www.warriorsssb.com",
                establishedYear = 2015,
                successRate = 68.4f,
                totalStudents = 4500
            ),
            CoachingInstitute(
                id = "centurion_defence",
                name = "Centurion Defence Academy",
                description = "Comprehensive SSB preparation with focus on NDA, CDS, and AFCAT aspirants. Offers integrated coaching for written exams and SSB together.",
                rating = 4.7f,
                reviewCount = 1340,
                location = "Anna Nagar, Chennai",
                city = "Chennai",
                state = "Tamil Nadu",
                type = InstituteType.BOTH,
                priceRange = PriceRange.MODERATE,
                specializations = listOf(
                    "NDA + SSB",
                    "CDS + SSB",
                    "AFCAT Preparation",
                    "Written + Interview"
                ),
                features = listOf(
                    "Integrated Courses",
                    "Written Exam Coaching",
                    "Current Affairs Classes",
                    "English Speaking",
                    "Mock Interviews"
                ),
                phoneNumber = "+91-44-26262888",
                email = "contact@centuriondefence.com",
                website = "www.centuriondefence.com",
                establishedYear = 2008,
                successRate = 80.2f,
                totalStudents = 9500
            ),
            CoachingInstitute(
                id = "vanguard_academy",
                name = "Vanguard SSB Academy",
                description = "Specialized in crash courses and last-minute SSB preparation. Ideal for candidates with SSB dates approaching who need intensive training.",
                rating = 4.2f,
                reviewCount = 620,
                location = "Shivaji Nagar, Pune",
                city = "Pune",
                state = "Maharashtra",
                type = InstituteType.PHYSICAL,
                priceRange = PriceRange.MODERATE,
                specializations = listOf(
                    "Crash Courses",
                    "15-Day Intensive",
                    "Last Minute Prep",
                    "Quick Revision"
                ),
                features = listOf(
                    "Fast-Track Training",
                    "Intensive Sessions",
                    "Expert Panel",
                    "Quick Tips & Tricks",
                    "Confidence Building"
                ),
                phoneNumber = "+91-20-25534466",
                email = "info@vanguardssb.com",
                website = "www.vanguardssb.com",
                establishedYear = 2012,
                successRate = 70.8f,
                totalStudents = 3800
            ),
            CoachingInstitute(
                id = "ace_the_ssb",
                name = "Ace The SSB",
                description = "Premium online platform with live interactive sessions and personalized coaching plans. Features India's largest question bank and adaptive learning algorithms.",
                rating = 4.6f,
                reviewCount = 1150,
                location = "Online Platform (HQ: Bangalore)",
                city = "Bangalore",
                state = "Karnataka",
                type = InstituteType.ONLINE,
                priceRange = PriceRange.PREMIUM,
                specializations = listOf(
                    "AI-Adaptive Learning",
                    "Live Interactive Classes",
                    "Personalized Plans",
                    "Video Lectures"
                ),
                features = listOf(
                    "200+ Hours Content",
                    "10,000+ Questions",
                    "Expert Q&A Forum",
                    "Mobile & Web Access",
                    "Lifetime Access"
                ),
                phoneNumber = "+91-80-41234567",
                email = "hello@acethessb.com",
                website = "www.acethessb.com",
                establishedYear = 2019,
                successRate = 75.6f,
                totalStudents = 7200
            ),
            CoachingInstitute(
                id = "brigadiers_coaching",
                name = "Brigadier's SSB Coaching",
                description = "Elite coaching by retired Brigadiers and Colonels. Luxury segment with small batch sizes and personalized mentoring for serious aspirants.",
                rating = 4.9f,
                reviewCount = 890,
                location = "Vasant Vihar, New Delhi",
                city = "New Delhi",
                state = "Delhi",
                type = InstituteType.PHYSICAL,
                priceRange = PriceRange.LUXURY,
                specializations = listOf(
                    "VIP Coaching",
                    "One-on-One Sessions",
                    "Personalized Roadmap",
                    "Premium Facilities"
                ),
                features = listOf(
                    "5 Students Per Batch",
                    "Personal Mentor",
                    "Luxury Accommodation",
                    "Gourmet Mess",
                    "Airport Pickup"
                ),
                phoneNumber = "+91-11-26151234",
                email = "admissions@brigadiersssb.com",
                website = "www.brigadiersssb.com",
                establishedYear = 2003,
                successRate = 92.3f,
                totalStudents = 2500
            )
        )
    }

    /**
     * Get unique cities for filter
     */
    fun getCities(): List<String> {
        return getInstitutes()
            .map { it.city }
            .distinct()
            .sorted()
    }
}

