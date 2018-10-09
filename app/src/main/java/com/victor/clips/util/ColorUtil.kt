package com.victor.clips.util

import com.victor.clips.R
import com.victor.clips.app.App
import java.util.*

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by longtv, All rights reserved.
 * -----------------------------------------------------------------
 * File: ColorUtil.java
 * Author: Victor
 * Date: 2018/9/4 9:40
 * Description: 
 * -----------------------------------------------------------------
 */
class ColorUtil {
    companion object {
        fun getDefaultRandomColor(): Int {
            val colors = intArrayOf(
                    App.instance().getResources().getColor(R.color.color_E6FCFC),
                    App.instance().getResources().getColor(R.color.color_E6FBFD),
                    App.instance().getResources().getColor(R.color.color_E5FAFE),
                    App.instance().getResources().getColor(R.color.color_E5F9FE))
            val random = Random()
            return colors[random.nextInt(colors.size)]
        }
    }
}