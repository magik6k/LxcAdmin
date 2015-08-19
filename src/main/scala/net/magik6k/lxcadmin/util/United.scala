package net.magik6k.lxcadmin.util

object United {
	def apply(value: Long, unit: Int = 1000, post: String = ""): String = {
		if (value < unit) return s"$value$post"
		val exp = (Math.log(value) / Math.log(unit)).floor
		val pre = "kMGTPE".charAt(exp.toInt-1)
		f"${value / Math.pow(unit, exp)}%.2f $pre$post"
	}
}
