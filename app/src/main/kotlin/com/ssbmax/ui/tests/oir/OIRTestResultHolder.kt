package com.ssbmax.ui.tests.oir

import com.ssbmax.core.domain.model.OIRTestResult

/**
 * Simple in-memory holder for passing OIRTestResult between screens
 * 
 * This is a temporary solution to avoid the PERMISSION_DENIED error when
 * trying to fetch results from Firestore before they're saved.
 * 
 * TODO: Replace with proper state management solution (ViewModel shared via NavBackStackEntry)
 */
object OIRTestResultHolder {
    private var _result: OIRTestResult? = null
    
    fun setResult(result: OIRTestResult) {
        _result = result
    }
    
    fun getResult(): OIRTestResult? = _result
    
    fun clearResult() {
        _result = null
    }
}


