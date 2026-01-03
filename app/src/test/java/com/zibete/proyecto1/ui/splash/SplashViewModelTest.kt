//package com.zibete.proyecto1.ui.splash
//
//import androidx.lifecycle.SavedStateHandle
//import com.google.firebase.auth.FirebaseUser
//import com.zibete.proyecto1.MainDispatcherRule
//import com.zibete.proyecto1.core.constants.Constants
//import com.zibete.proyecto1.core.utils.AppChecksProvider
//import com.zibete.proyecto1.core.utils.ZibeResult
//import com.zibete.proyecto1.data.UserPreferencesActions
//import com.zibete.proyecto1.data.UserPreferencesProvider
//import com.zibete.proyecto1.data.UserSessionProvider
//import com.zibete.proyecto1.domain.session.LogoutUseCase
//import com.zibete.proyecto1.domain.session.SessionBootstrapper
//import com.zibete.proyecto1.fakes.FakeAppChecksProvider
//import com.zibete.proyecto1.fakes.FakeLogoutUseCase
//import com.zibete.proyecto1.fakes.FakeSessionBootstrapper
//import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
//import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
//import com.zibete.proyecto1.fakes.FakeUserSessionProvider
//import com.zibete.proyecto1.testing.TestData
//import com.zibete.proyecto1.testing.UnitScenario
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.every
//import io.mockk.mockk
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.async
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.test.advanceUntilIdle
//import kotlinx.coroutines.test.runCurrent
//import kotlinx.coroutines.test.runTest
//import kotlinx.coroutines.withTimeout
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertTrue
//import org.junit.Rule
//import org.junit.Test
//
//@OptIn(ExperimentalCoroutinesApi::class)
//class SplashViewModelTest {
//
//    @get:Rule
//    val mainDispatcherRule = MainDispatcherRule()
//
//    // ---------------------------------------------------------------------------------
//    // Onboarding on/off
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando onboarding no esta hecho navega a onboarding`() = runTest {
//        // Given
//        val unitScenario = UnitScenario(
//            onboardingDone = false
//        )
//
//        val vm = buildVm(
//            userPreferencesProvider = FakeUserPreferencesProvider { unitScenario },
//            userPreferencesActions = FakeUserPreferencesActions { unitScenario }
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.start(context = mockk(relaxed = true))
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals((SplashUiEvent.NavigateOnBoarding), event)
//        assertTrue(unitScenario.onboardingDone)
//    }
//
//    @Test
//    fun `cuando onboarding esta hecho no navega a onboarding`() = runTest {
//        // Given
//        val unitScenario = UnitScenario()
//
//        val vm = buildVm(
//            userPreferencesProvider = FakeUserPreferencesProvider { unitScenario },
//            userPreferencesActions = FakeUserPreferencesActions { unitScenario }
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.start(context = mockk(relaxed = true))
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals(SplashUiEvent.NavigateAuth, event)
//        assertTrue(unitScenario.onboardingDone)
//    }
//
//    // ---------------------------------------------------------------------------------
//    // Session conflict on/off
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando hay conflicto de sesion muestra dialogo`() = runTest {
//        // Given
//        val savedStateHandle = SavedStateHandle()
//
//        savedStateHandle[Constants.EXTRA_SESSION_CONFLICT] = true
//
//        val fakeUserSessionProvider = FakeUserSessionProvider(
//            currentUser = mockk(relaxed = true)
//        )
//
//        val vm = buildVm(
//            savedStateHandle = savedStateHandle,
//            userSessionProvider = fakeUserSessionProvider
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.start(context = mockk(relaxed = true))
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals((SplashUiEvent.ShowSessionConflictDialog), event)
//    }
//
//    @Test
//    fun `cuando se confirma conflicto de sesion continua a main`() = runTest {
//        // Given
//        val fakeUserSessionProvider = FakeUserSessionProvider(
//            currentUser = mockk(relaxed = true)
//        )
//
//        val fakeSessionBootstrapper = mockk<SessionBootstrapper> {
//            coEvery { bootstrap(any(), any(), any()) } returns ZibeResult.Success(Unit)
//        }
//
//        val vm = buildVm(
//            userSessionProvider = fakeUserSessionProvider,
//            sessionBootstrapper = fakeSessionBootstrapper
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.onSessionConflictConfirmed()
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals((SplashUiEvent.NavigateMain), event) // Navegó a Main
//        coVerify(exactly = 0) { fakeSessionBootstrapper.bootstrap(any(), any(), any()) }   // no se llamó
//    }
//
//    @Test
//    fun `cuando se cancela conflicto de sesion hace logout y navega a auth`() = runTest {
//        // Given
//        val fakeUserSessionProvider = FakeUserSessionProvider(
//            currentUser = mockk(relaxed = true)
//        )
//
//        val fakeSessionBootstrapper = mockk<SessionBootstrapper> {
//            coEvery { bootstrap(any(), any(), any()) } returns ZibeResult.Success(Unit)
//        }
//
//        val vm = buildVm(
//            userSessionProvider = fakeUserSessionProvider,
//            sessionBootstrapper = fakeSessionBootstrapper
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.onSessionConflictCancelled()
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals((SplashUiEvent.NavigateAuth), event)
//        coVerify(exactly = 1) { fakeSessionBootstrapper.bootstrap(any(), any(), any()) }
//    }
//
//    // ---------------------------------------------------------------------------------
//    // Internet conflict
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando no hay internet muestra dialogo`() = runTest {
//        // Given
//        val unitScenario = UnitScenario(
//            hasInternet = false
//        )
//
//        val fakeSessionBootstrapper = mockk<SessionBootstrapper> {
//            coEvery { bootstrap(any(), any(), any()) } returns ZibeResult.Success(Unit)
//        }
//
//        val vm = buildVm(
//            userPreferencesProvider = FakeUserPreferencesProvider { unitScenario },
//            userPreferencesActions = FakeUserPreferencesActions { unitScenario },
//            sessionBootstrapper = fakeSessionBootstrapper
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.start(context = mockk(relaxed = true))
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals((SplashUiEvent.ShowNoInternetDialog), event)
//        coVerify(exactly = 0) { fakeSessionBootstrapper.bootstrap(any(), any(), any()) }
//    }
//
//    // ---------------------------------------------------------------------------------
//    // permission
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando hay usuario pero no hay permiso de ubicacion solicita permiso`() = runTest {
//        // Given
//        val unitScenario = UnitScenario(
//            hasLocationPermission = false
//        )
//
//        val fakeUserSessionProvider = FakeUserSessionProvider(
//            currentUser = mockk(relaxed = true)
//        )
//
//        val fakeSessionBootstrapper = mockk<SessionBootstrapper> {
//            coEvery { bootstrap(any(), any(), any()) } returns ZibeResult.Success(Unit)
//        }
//
//        val vm = buildVm(
//            userPreferencesProvider = FakeUserPreferencesProvider { unitScenario },
//            userPreferencesActions = FakeUserPreferencesActions { unitScenario },
//            userSessionProvider = fakeUserSessionProvider,
//            sessionBootstrapper = fakeSessionBootstrapper
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.start(context = mockk(relaxed = true))
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//        assertEquals((SplashUiEvent.RequestLocationPermission), event)
//        coVerify(exactly = 0) { fakeSessionBootstrapper.bootstrap(any(), any(), any()) }
//    }
//
//    // ---------------------------------------------------------------------------------
//    // Helper logout
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando se solicita logout navega a auth`() = runTest {
//        // Given
//        val vm = buildVm()
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.onLogoutRequested()
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//        assertEquals((SplashUiEvent.NavigateAuth), event)
//
//    }
//
//    // ---------------------------------------------------------------------------------
//    // user null
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando no hay usuario navega a auth`() = runTest {
//        // Given
//        val fakeUserSessionProvider = FakeUserSessionProvider(
//            currentUser = null
//        )
//
//        val fakeSessionBootstrapper = mockk<SessionBootstrapper> {
//            coEvery { bootstrap(any(), any(), any()) } returns ZibeResult.Success(Unit)
//        }
//
//        val vm = buildVm(
//            userSessionProvider = fakeUserSessionProvider,
//            sessionBootstrapper = fakeSessionBootstrapper
//        )
//
//        val deferred = async { awaitEvent(vm) }
//        runCurrent()
//
//        // When
//        vm.start(context = mockk(relaxed = true))
//        advanceUntilIdle()
//
//        // Then
//        val event = deferred.await()
//
//        assertEquals((SplashUiEvent.NavigateAuth), event)
//        coVerify(exactly = 0) { fakeSessionBootstrapper.bootstrap(any(), any(), any()) }
//    }
//
//    // ---------------------------------------------------------------------------------
//    // user OK
//    // ---------------------------------------------------------------------------------
//
//    @Test
//    fun `cuando internet ok + user ok + location ok llama SessionBootstrapper con el uid correcto`() =
//        runTest {
//            // Given
//            val fakeUserSessionProvider = FakeUserSessionProvider(
//                currentUser = mockk<FirebaseUser> { every { uid } returns TestData.UID }
//            )
//
//            val fakeSessionBootstrapper = mockk<SessionBootstrapper> {
//                coEvery { bootstrap(any(), any(), any()) } returns ZibeResult.Success(Unit)
//            }
//
//            val vm = buildVm(
//                userSessionProvider = fakeUserSessionProvider,
//                sessionBootstrapper = fakeSessionBootstrapper
//            )
//
//            val deferred = async { awaitEvent(vm) }
//            runCurrent()
//
//            // When
//            vm.start(context = mockk(relaxed = true))
//            advanceUntilIdle()
//
//            // Then
//            val event = deferred.await()
//
//            assertEquals((SplashUiEvent.NavigateMain), event)
//            coVerify(exactly = 1) { fakeSessionBootstrapper.bootstrap(TestData.UID,any(),any()) }
//        }
//
//    private suspend fun awaitEvent(vm: SplashViewModel): SplashUiEvent {
//        return withTimeout(2_000) { vm.events.first() }
//    }
//
//    private fun buildVm(
//        savedStateHandle : SavedStateHandle = SavedStateHandle(),
//        appChecksProvider : AppChecksProvider = FakeAppChecksProvider { UnitScenario() },
//        userSessionProvider : UserSessionProvider = FakeUserSessionProvider(),
//        userPreferencesProvider : UserPreferencesProvider = FakeUserPreferencesProvider { UnitScenario() },
//        userPreferencesActions : UserPreferencesActions = FakeUserPreferencesActions { UnitScenario() },
//        sessionBootstrapper : SessionBootstrapper = FakeSessionBootstrapper(),
//        logoutUseCase : LogoutUseCase = FakeLogoutUseCase()
//    ) : SplashViewModel {
//        return SplashViewModel(
//            savedStateHandle = savedStateHandle,
//            appChecksProvider = appChecksProvider,
//            userSessionProvider = userSessionProvider,
//            userPreferencesProvider = userPreferencesProvider,
//            userPreferencesActions = userPreferencesActions,
//            sessionBootstrapper = sessionBootstrapper,
//            logoutUseCase = logoutUseCase
//        )
//    }
//
//}