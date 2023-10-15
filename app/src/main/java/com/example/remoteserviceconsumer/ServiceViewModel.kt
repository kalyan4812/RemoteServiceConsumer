package com.example.remoteserviceconsumer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ServiceViewModel : ViewModel() {


    val _serviceBind = MutableLiveData(false)


    val _serviceUnBind = MutableLiveData(false)

    val _getNumbers = MutableLiveData(false)

    val currentRandomValue = MutableLiveData<Int>(0)


    fun setServiceBind() {
        _serviceBind.value = true
    }

    fun setServiceUnBind() {
        _serviceUnBind.value = true
    }

    fun setRandomNumberFetching() {
        _getNumbers.value = true
    }

    fun setRandomValue(value: Int) {
        currentRandomValue.postValue(value)
    }


}