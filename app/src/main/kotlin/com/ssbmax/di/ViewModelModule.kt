package com.ssbmax.di

import com.ssbmax.MainViewModel
import com.ssbmax.ui.AppViewModel
import com.ssbmax.ui.analytics.AnalyticsViewModel
import com.ssbmax.ui.auth.AuthViewModel
import com.ssbmax.ui.debug.MemoryLeakTestViewModel
import com.ssbmax.ui.faq.FAQViewModel
import com.ssbmax.ui.grading.TestDetailGradingViewModel
import com.ssbmax.ui.home.instructor.InstructorHomeViewModel
import com.ssbmax.ui.home.student.StudentHomeViewModel
import com.ssbmax.ui.instructor.InstructorGradingViewModel
import com.ssbmax.ui.interview.result.InterviewResultViewModel
import com.ssbmax.ui.interview.session.InterviewSessionViewModel
import com.ssbmax.ui.interview.start.StartInterviewViewModel
import com.ssbmax.ui.marketplace.MarketplaceViewModel
import com.ssbmax.ui.notifications.NotificationCenterViewModel
import com.ssbmax.ui.phase.Phase1DetailViewModel
import com.ssbmax.ui.phase.Phase2DetailViewModel
import com.ssbmax.ui.profile.StudentProfileViewModel
import com.ssbmax.ui.profile.UserProfileViewModel
import com.ssbmax.ui.results.HistoricResultsViewModel
import com.ssbmax.ui.settings.SettingsViewModel
import com.ssbmax.ui.settings.SubscriptionManagementViewModel
import com.ssbmax.ui.settings.notifications.NotificationSettingsViewModel
import com.ssbmax.ui.settings.theme.ThemeSettingsViewModel
import com.ssbmax.ui.splash.SplashViewModel
import com.ssbmax.ui.ssboverview.SSBOverviewViewModel
import com.ssbmax.ui.study.StudyMaterialDetailViewModel
import com.ssbmax.ui.study.StudyMaterialsViewModel
import com.ssbmax.ui.submissions.SubmissionDetailViewModel
import com.ssbmax.ui.submissions.SubmissionsListViewModel
import com.ssbmax.ui.tests.StudentTestsViewModel
import com.ssbmax.ui.tests.gpe.GPESubmissionResultViewModel
import com.ssbmax.ui.tests.gpe.GPETestViewModel
import com.ssbmax.ui.tests.gto.gd.GDResultViewModel
import com.ssbmax.ui.tests.gto.gd.GDTestViewModel
import com.ssbmax.ui.tests.gto.lecturette.LecturetteResultViewModel
import com.ssbmax.ui.tests.gto.lecturette.LecturetteTestViewModel
import com.ssbmax.ui.tests.oir.OIRDebugViewModel
import com.ssbmax.ui.tests.oir.OIRSubmissionResultViewModel
import com.ssbmax.ui.tests.oir.OIRTestViewModel
import com.ssbmax.ui.tests.piq.PIQSubmissionResultViewModel
import com.ssbmax.ui.tests.piq.PIQTestViewModel
import com.ssbmax.ui.tests.ppdt.PPDTSubmissionResultViewModel
import com.ssbmax.ui.tests.ppdt.PPDTTestViewModel
import com.ssbmax.ui.tests.sdt.SDTSubmissionResultViewModel
import com.ssbmax.ui.tests.sdt.SDTTestViewModel
import com.ssbmax.ui.tests.srt.SRTSubmissionResultViewModel
import com.ssbmax.ui.tests.srt.SRTTestViewModel
import com.ssbmax.ui.tests.tat.TATSubmissionResultViewModel
import com.ssbmax.ui.tests.tat.TATTestViewModel
import com.ssbmax.ui.tests.wat.WATSubmissionResultViewModel
import com.ssbmax.ui.tests.wat.WATTestViewModel
import com.ssbmax.ui.topic.TopicViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.ssbmax.ui.premium.UpgradeViewModel as PremiumUpgradeViewModel
import com.ssbmax.ui.upgrade.UpgradeViewModel as UpgradeUpgradeViewModel

/**
 * Koin bindings for all 55 app ViewModels, converted from `@HiltViewModel`.
 * `viewModelOf` resolves each constructor via reflection against the Koin
 * graph (repositories/use cases from :shared and :core:data, plus this
 * package's own use-case/manager bindings) and integrates with
 * `koinViewModel()` at Compose call sites, including `SavedStateHandle`
 * injection where a ViewModel takes one.
 */
val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AppViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::MemoryLeakTestViewModel)
    viewModelOf(::FAQViewModel)
    viewModelOf(::TestDetailGradingViewModel)
    viewModelOf(::InstructorHomeViewModel)
    viewModelOf(::StudentHomeViewModel)
    viewModelOf(::InstructorGradingViewModel)
    viewModelOf(::InterviewResultViewModel)
    viewModelOf(::InterviewSessionViewModel)
    viewModelOf(::StartInterviewViewModel)
    viewModelOf(::MarketplaceViewModel)
    viewModelOf(::NotificationCenterViewModel)
    viewModelOf(::Phase1DetailViewModel)
    viewModelOf(::Phase2DetailViewModel)
    viewModelOf(::PremiumUpgradeViewModel)
    viewModelOf(::StudentProfileViewModel)
    viewModelOf(::UserProfileViewModel)
    viewModelOf(::HistoricResultsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SubscriptionManagementViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::ThemeSettingsViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::SSBOverviewViewModel)
    viewModelOf(::StudyMaterialDetailViewModel)
    viewModelOf(::StudyMaterialsViewModel)
    viewModelOf(::SubmissionDetailViewModel)
    viewModelOf(::SubmissionsListViewModel)
    viewModelOf(::StudentTestsViewModel)
    viewModelOf(::GPESubmissionResultViewModel)
    viewModelOf(::GPETestViewModel)
    viewModelOf(::GDResultViewModel)
    viewModelOf(::GDTestViewModel)
    viewModelOf(::LecturetteResultViewModel)
    viewModelOf(::LecturetteTestViewModel)
    viewModelOf(::OIRDebugViewModel)
    viewModelOf(::OIRSubmissionResultViewModel)
    viewModelOf(::OIRTestViewModel)
    viewModelOf(::PIQSubmissionResultViewModel)
    viewModelOf(::PIQTestViewModel)
    viewModelOf(::PPDTSubmissionResultViewModel)
    viewModelOf(::PPDTTestViewModel)
    viewModelOf(::SDTSubmissionResultViewModel)
    viewModelOf(::SDTTestViewModel)
    viewModelOf(::SRTSubmissionResultViewModel)
    viewModelOf(::SRTTestViewModel)
    viewModelOf(::TATSubmissionResultViewModel)
    viewModelOf(::TATTestViewModel)
    viewModelOf(::WATSubmissionResultViewModel)
    viewModelOf(::WATTestViewModel)
    viewModelOf(::TopicViewModel)
    viewModelOf(::UpgradeUpgradeViewModel)
}
