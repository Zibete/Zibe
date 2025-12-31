package com.zibete.proyecto1.ui.signup

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.data.SessionRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.testing.TestData
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.ui.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.ui.constants.ERR_EMAIL_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_PASSWORD_REQUIRED
import com.zibete.proyecto1.ui.constants.ERR_UNDER_AGE
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_BIRTHDAY_REQUIRED
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_EMAIL_IN_USE
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_NAME_REQUIRED
import com.zibete.proyecto1.ui.constants.SIGNUP_ERR_UNEXPECTED_PREFIX
import com.zibete.proyecto1.ui.constants.SIGNUP_MSG_SUCCESS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userRepository: UserRepository
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var sessionRepository: SessionRepository

    @Before
    fun setup() {
        firebaseAuth = mock(FirebaseAuth::class.java)
        userRepository = mock(UserRepository::class.java)
        userSessionManager = mock(UserSessionManager::class.java)
        sessionRepository = mock(SessionRepository::class.java)
    }

    @Test
    fun `onRegister blank email emits warning and does not call auth`() = runTest {
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = "",
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )

        val event = deferred.await()
        assertTrue(event is SignUpUiEvent.ShowSnack)

        val snack = event as SignUpUiEvent.ShowSnack
        assertEquals(ERR_EMAIL_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        verify(firebaseAuth, never()).createUserWithEmailAndPassword(anyString(), anyString())
        verifyNoInteractions(userRepository, userSessionManager, sessionRepository)
        verify(userSessionManager, never()).updateAuthProfile(anyString(), anyString())
        verify(sessionRepository, never()).setActiveSession(anyString(), anyString(),anyString())
    }

    @Test
    fun `onRegister blank password emits warning`() = runTest {
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = "",
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )

        val event = deferred.await()
        assertTrue(event is SignUpUiEvent.ShowSnack)

        val snack = event as SignUpUiEvent.ShowSnack
        assertEquals(ERR_PASSWORD_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        verify(firebaseAuth, never()).createUserWithEmailAndPassword(anyString(), anyString())
    }

    @Test
    fun `onRegister blank name emits warning`() = runTest {
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = "",
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )

        val event = deferred.await()
        assertTrue(event is SignUpUiEvent.ShowSnack)

        val snack = event as SignUpUiEvent.ShowSnack
        assertEquals(SIGNUP_ERR_NAME_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        verify(firebaseAuth, never()).createUserWithEmailAndPassword(anyString(), anyString())

    }

    @Test
    fun `onRegister blank birthday emits warning`() = runTest {
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = "",
            description = TestData.DESC
        )

        val event = deferred.await()
        assertTrue(event is SignUpUiEvent.ShowSnack)

        val snack = event as SignUpUiEvent.ShowSnack
        assertEquals(SIGNUP_ERR_BIRTHDAY_REQUIRED, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        verify(firebaseAuth, never()).createUserWithEmailAndPassword(anyString(), anyString())

    }

    @Test
    fun `onRegister under age emits warning`() = runTest {
        val vm = buildVm()

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = "2015-01-01",
            description = TestData.DESC
        )

        val event = deferred.await()
        assertTrue(event is SignUpUiEvent.ShowSnack)

        val snack = event as SignUpUiEvent.ShowSnack
        assertEquals(ERR_UNDER_AGE, snack.message)
        assertEquals(ZibeSnackType.WARNING, snack.type)
        assertFalse(vm.uiState.value.isLoading)

        verify(firebaseAuth, never()).createUserWithEmailAndPassword(anyString(), anyString())
    }

    @Test
    fun `onRegister success emits success and writes session when installId present`() = runTest {
        val vm = buildVm()

        val firebaseUser = mock(FirebaseUser::class.java)
        `when`(firebaseUser.uid).thenReturn(TestData.UID)

        val authResult = mock(AuthResult::class.java)
        `when`(authResult.user).thenReturn(firebaseUser)

        `when`(firebaseAuth.createUserWithEmailAndPassword(eq(TestData.EMAIL), eq(TestData.PASSWORD)))
            .thenReturn(Tasks.forResult(authResult))

        `when`(sessionRepository.getLocalInstallId()).thenReturn(TestData.INSTALL_ID)
        `when`(sessionRepository.getLocalFcmToken()).thenReturn(TestData.TOKEN)

        val first = async { awaitEvent(vm) }
        val second = async { withTimeout(2_000) { vm.events.drop(1).first() } }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        verify(firebaseAuth).createUserWithEmailAndPassword(TestData.EMAIL, TestData.PASSWORD)
        verify(userSessionManager).updateAuthProfile(TestData.NAME,DEFAULT_PROFILE_PHOTO_URL)
        verify(userRepository).createUserNode(firebaseUser, TestData.BIRTH, TestData.DESC)
        verify(sessionRepository).setActiveSession(TestData.UID,TestData.INSTALL_ID,TestData.TOKEN)

        val e1 = first.await()
        val e2 = second.await()

        assertEquals(SIGNUP_MSG_SUCCESS, (e1 as SignUpUiEvent.ShowSnack).message)
        assertEquals(ZibeSnackType.SUCCESS, e1.type)
        assertEquals(SignUpUiEvent.NavigateToSplash, e2)

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onRegister success does not write session when installId blank`() = runTest {
        val vm = buildVm()

        val firebaseUser = mock(FirebaseUser::class.java)
        `when`(firebaseUser.uid).thenReturn(TestData.UID)

        val authResult = mock(AuthResult::class.java)
        `when`(authResult.user).thenReturn(firebaseUser)

        `when`(firebaseAuth.createUserWithEmailAndPassword(eq(TestData.EMAIL), eq(TestData.PASSWORD)))
            .thenReturn(Tasks.forResult(authResult))

        `when`(sessionRepository.getLocalInstallId()).thenReturn("") //installId blank
        `when`(sessionRepository.getLocalFcmToken()).thenReturn(TestData.TOKEN)

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        val event = deferred.await()
        val snack = event as SignUpUiEvent.ShowSnack

        verify(sessionRepository, never()).setActiveSession(anyString(), anyString(), anyString())

        assertEquals(SIGNUP_MSG_SUCCESS, event.message)
        assertEquals(ZibeSnackType.SUCCESS, snack.type)

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onRegister firebase email already in use emits error snack`() = runTest {
        val vm = buildVm()

        val ex = mock(FirebaseAuthException::class.java)
        `when`(ex.errorCode).thenReturn("ERROR_EMAIL_ALREADY_IN_USE")

        `when`(firebaseAuth.createUserWithEmailAndPassword(eq(TestData.EMAIL), eq(TestData.PASSWORD)))
            .thenReturn(Tasks.forException(ex))

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        val event = deferred.await()
        val snack = event as SignUpUiEvent.ShowSnack

        assertEquals(SIGNUP_ERR_EMAIL_IN_USE, snack.message)
        assertEquals(ZibeSnackType.ERROR, snack.type)
        assertFalse(vm.uiState.value.isLoading)

    }

    @Test
    fun `onRegister generic exception emits unexpected error snack`() = runTest {
        val vm = buildVm()

        val ex = IllegalStateException("boom")

        `when`(firebaseAuth.createUserWithEmailAndPassword(eq(TestData.EMAIL), eq(TestData.PASSWORD)))
            .thenReturn(Tasks.forException(ex))

        val deferred = async { awaitEvent(vm) }
        runCurrent()

        vm.onRegister(
            email = TestData.EMAIL,
            password = TestData.PASSWORD,
            name = TestData.NAME,
            birthDate = TestData.BIRTH,
            description = TestData.DESC
        )
        advanceUntilIdle()

        val event = deferred.await()
        val snack = event as SignUpUiEvent.ShowSnack


        assertTrue(snack.message.startsWith(SIGNUP_ERR_UNEXPECTED_PREFIX))
        assertEquals(ZibeSnackType.ERROR, snack.type)
        assertFalse(vm.uiState.value.isLoading)
    }

    private suspend fun awaitEvent(vm: SignUpViewModel): SignUpUiEvent {
        return withTimeout(2_000) { vm.events.first() }
    }

    private fun buildVm(): SignUpViewModel {
        return SignUpViewModel(
            firebaseAuth = firebaseAuth,
            userRepository = userRepository,
            userSessionManager = userSessionManager,
            sessionRepository = sessionRepository
        )
    }
}
