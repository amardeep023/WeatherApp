package com.weatherapp.data.local.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.weatherapp.data.local.Constants.DEGREE
import com.weatherapp.data.local.Constants.IMAGE_URL
import com.weatherapp.data.local.Constants.SIZE
import com.weatherapp.data.model.forecast.ListItem
import com.weatherapp.data.local.ui.theme.SMALL_MARGIN

@ExperimentalCoilApi
@Composable
fun ListTodayWeather(forecast: ListItem) {

    Card(
        shape = RoundedCornerShape(30.dp),
        elevation = 10.dp,
        modifier = Modifier
            .padding(8.dp)
            .size(60.dp, 110.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {

            val lastIndex = forecast.main?.temp.toString().indexOf(".")

            Image(
                painter = rememberAsyncImagePainter(
                    model = "$IMAGE_URL${
                        forecast.weather?.get(0)?.icon
                    }$SIZE"
                ),
                contentDescription = "",
                modifier = Modifier
                    .size(40.dp)
            )

            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = forecast.main?.temp.toString()
                    .substring(0, lastIndex) + " " + DEGREE,
                color = MaterialTheme.colors.primaryVariant,
                fontSize = 16.sp,

                )
            Spacer(modifier = Modifier.padding(2.dp))

            Text(
                text = forecast.dt_txt?.substring(11, 16).toString(),
                color = MaterialTheme.colors.primaryVariant,
                fontSize = 16.sp

                )
        }

    }
}
