package com.weatherapp.data.local.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.weatherapp.R
import com.weatherapp.data.local.Constants.C_UNIT
import com.weatherapp.data.local.Constants.DETAILS_SCREEN
import com.weatherapp.data.local.Constants.FORMAT_TYPE
import com.weatherapp.data.local.Constants.F_UNIT
import com.weatherapp.data.local.Constants.IMAGE_URL
import com.weatherapp.data.local.Constants.METRIC
import com.weatherapp.data.local.Constants.SIZE
import com.weatherapp.data.local.Constants.TWELVE_PM
import com.weatherapp.data.model.forecast.FiveDaysForecastResponse
import com.weatherapp.data.model.forecast.ListItem
import com.weatherapp.data.model.geocoding.GeocodingResponse
import com.weatherapp.data.model.weather.CurrentWeatherResponse
import com.weatherapp.data.network.Resource
import com.weatherapp.data.viewmodel.HomeViewModel
import com.weatherapp.data.local.ui.screen.search.ListWeatherForecast
import com.weatherapp.data.local.ui.theme.BIG_MARGIN
import com.weatherapp.data.local.ui.theme.LARGE_MARGIN
import com.weatherapp.data.local.ui.theme.MEDIUM_MARGIN
import com.weatherapp.data.local.ui.theme.SMALL_MARGIN
import com.weatherapp.data.local.ui.theme.VERY_SMALL_MARGIN
import com.weatherapp.ui.theme.*
import com.weatherapp.util.Circle
import com.weatherapp.util.RequestState
import com.weatherapp.util.formatDate
import com.weatherapp.util.handleApiError

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavHostController
) {

    val forecastState =
        remember { mutableStateOf<List<ListItem>>(listOf()) }
    val apiError = remember { mutableStateOf("") }


    viewModel.requestState.value.also { state ->
        when (state) {
            RequestState.IDLE -> {
                LoadingScreen()
            }

            RequestState.COMPLETE -> {
                UpdateUi(
                    viewModel,
                    apiError,
                    navController,
                    forecastState,
                )
            }

            RequestState.ERROR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                        .padding(start = LARGE_MARGIN, end = LARGE_MARGIN),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = apiError.value,
                        color = MaterialTheme.colors.primaryVariant,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalCoilApi::class)
@Composable
fun UpdateUi(
    viewModel: HomeViewModel,
    apiError: MutableState<String>,
    navController: NavHostController,
    forecastState: MutableState<List<ListItem>>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = BIG_MARGIN)
    ) {

        item {
            Header(
                viewModel,
                apiError,
                navController,
                forecastState,
            )
        }

        // Forecast in next 5 days
        if (forecastState.value.isNotEmpty()) {
            var savedDate: String? = null
            val savedTime = TWELVE_PM

            // Display the list in 5 days forecast
            itemsIndexed(forecastState.value) { index, item ->
                // Save date and time (first time only)
                if (index == 0) {
                    savedDate = forecastState.value[index].dt_txt
                }

                // Extract date and time to use it in comparison
                val currentDate =
                    forecastState.value[index].dt_txt?.substring(0, 10)
                val currentTime =
                    forecastState.value[index].dt_txt?.substring(11, 16)

                // Display the next 5 days forecast on 3:00 pm only
                if (currentDate != savedDate && currentTime == savedTime) {
                    savedDate = currentDate
                    ListWeatherForecast(
                        forecast = item,
                        timeVisibility = true
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


@ExperimentalCoilApi
@Composable
fun Header(
    viewModel: HomeViewModel,
    apiError: MutableState<String>,
    navController: NavHostController,
    forecastState: MutableState<List<ListItem>>,
) {
    val context = LocalContext.current
    var data: CurrentWeatherResponse? = null
    var windSpeed: Int? = null
    var latitude = 0.0
    var longitude = 0.0


    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .padding(bottom = MEDIUM_MARGIN, start = 8.dp, end = 8.dp)
    ) {


        // Observe on state flow values from view models
        val geocodingResponse by viewModel.geocoding.collectAsState()
        val weatherResponse by viewModel.currentWeather.collectAsState()
        val forecastResponse by viewModel.weatherForecast.collectAsState()


        // Observe on geocoding response from api
        if (geocodingResponse is Resource.Success) {
            val response =
                geocodingResponse as Resource.Success<List<GeocodingResponse>>
            response.value.also { geocoding ->
                if (geocoding.isNotEmpty()) {
                    latitude = geocoding[0].lat ?: 0.0
                    longitude = geocoding[0].lon ?: 0.0
                    LaunchedEffect(key1 = true) {
                        viewModel.initCurrentWeather(latitude, longitude)
                    }

                } else {
                    apiError.value =
                        context.getString(R.string.invalid_city)
                    viewModel.requestState.value = RequestState.ERROR
                }
            }
        } else if (geocodingResponse is Resource.Failure) {
            context.handleApiError(geocodingResponse as Resource.Failure)
            apiError.value = context.getString(R.string.connection_error)
            viewModel.requestState.value = RequestState.ERROR
        }


        // Observe on weather response from api
        if (weatherResponse is Resource.Success) {
            data =
                (weatherResponse as Resource.Success<CurrentWeatherResponse>).value
            windSpeed =
                data?.wind?.speed?.times(60)?.times(60)?.div(1000)?.toInt()
        } else if (weatherResponse is Resource.Failure) {
            context.handleApiError(weatherResponse as Resource.Failure)
        }


        // Observe on forecast response from api
        if (forecastResponse is Resource.Success) {
            forecastState.value =
                (forecastResponse as Resource.Success<FiveDaysForecastResponse>).value.list!!
        } else if (forecastResponse is Resource.Failure) {
            LaunchedEffect(key1 = true) {
                context.handleApiError(forecastResponse as Resource.Failure)
            }
        }


        if (data != null) {
            val unitLetter =
                if (viewModel.tempUnit == METRIC) C_UNIT else F_UNIT
            val lastIndex = data?.main?.temp.toString().indexOf(".")

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_location),
                    contentDescription = "",
                    tint = MaterialTheme.colors.primary,

                    )
                // Current location
                Text(
                    text = data?.name.toString(),
                    color = MaterialTheme.colors.primary,
                    fontSize = 18.sp,

                    )
            }

            Spacer(modifier = Modifier.padding(bottom = 50.dp))




            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LARGE_MARGIN, end = LARGE_MARGIN)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colors.onBackground),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Temperature
                Text(
                    text = "${
                        data?.main?.temp.toString().substring(0, lastIndex)
                    }$unitLetter",
                    fontSize = 90.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colors.primary
                )

                // Feels like
                Text(
                    text = "${stringResource(id = R.string.feels_like)} ${
                        data?.main?.feels_like.toString()
                            .substring(0, lastIndex)
                    }$unitLetter",
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.primaryVariant,
                    modifier = Modifier.padding(top = SMALL_MARGIN)
                )

                // Weather description
                Text(
                    text = data?.weather?.get(0)?.description.toString(),
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.padding(top = VERY_SMALL_MARGIN)
                )

                Image(
                    painter = rememberAsyncImagePainter(
                        model = "$IMAGE_URL${data?.weather?.get(0)?.icon}$SIZE"
                    ),
                    contentDescription = "",
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.padding(bottom = 16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    //Wind Icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_wind),
                        contentDescription = "",
                        tint = MaterialTheme.colors.secondary,

                        )
                    // Wind speed
                    Text(
                        text = "$windSpeed ${stringResource(id = R.string.km_h)}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.primaryVariant,

                        )
                }


                // More
                Button(
                    onClick = {
                        navController.navigate(
                            "$DETAILS_SCREEN/${longitude.toFloat()}/${latitude.toFloat()}/${data?.clouds?.all}"
                        )
                    },
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                ) {

                    Text(
                        text = stringResource(R.string.more),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.primaryVariant,
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_keyboard_arrow_right),
                        contentDescription = "",
                        alpha = 0.7f
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(horizontal = 16.dp)

            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_visibility),
                    contentDescription = "",
                    tint = MaterialTheme.colors.secondary,
                    modifier = Modifier

                )

                // Visibility
                Text(
                    text = "${stringResource(id = R.string.visibility)} ${
                        data?.visibility?.div(
                            1000
                        )
                    } ${
                        stringResource(id = R.string.km)
                    }",
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.primaryVariant,
                    modifier = Modifier

                )
            }

            Spacer(modifier = Modifier.padding(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {

                // Humidity title
                Text(
                    text = stringResource(R.string.humidity),
                    color = MaterialTheme.colors.primaryVariant,
                    fontSize = 16.sp,
                )

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Humidity circle
                    Circle(
                        modifier = Modifier,

                        if (data?.main?.humidity != null) {
                            data?.main?.humidity?.toDouble()?.div(100)!!
                                .toFloat()
                        } else {
                            0f
                        }, MaterialTheme.colors.secondary
                    )


                }

            }


            // Today date
            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primary
                        )
                    ) {
                        append("${stringResource(id = R.string.today)}\n")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colors.primaryVariant,
                            fontSize = 14.sp
                        )
                    ) {
                        append(formatDate(FORMAT_TYPE))
                    }
                }, modifier = Modifier.padding(horizontal = 16.dp)

                )

//             Today weather every 3 hours
            LazyRow(
                modifier = Modifier
                    .padding(start = MEDIUM_MARGIN, end = MEDIUM_MARGIN)
            ) {
                itemsIndexed(forecastState.value) { index, item ->
                    if (index <= 8) {
                        ListTodayWeather(forecast = item)
                    }
                }
            }
        }
    }
}