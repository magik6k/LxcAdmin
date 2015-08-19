package net.magik6k.lxcadmin.widget

import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.widgets.basic.{ProgressBar, TextLabel}
import net.magik6k.jwwf.widgets.basic.panel.{Panel, Row}
import net.magik6k.lxcadmin.util.Using

import scala.io.Source

object MemMonProvider {
	var total = 0L
	var free = 0L

	var swap = 0L
	var swapFree = 0L

	def refresh() {
		var res = Map.empty[String, String]
		val reg = """([^:]+):\s+(.+)""".r

		Using(Source.fromFile("/proc/meminfo")){_.getLines().foreach {
			case reg(key, value) => res += key -> value.trim
			case _ =>
		}}

		total = res("MemTotal").takeWhile(_ != ' ').toLong
		free = res("MemAvailable").takeWhile(_ != ' ').toLong

		swap = res("SwapTotal").takeWhile(_ != ' ').toLong
		swapFree = res("SwapFree").takeWhile(_ != ' ').toLong
	}
}

class LocalMemMon extends Row(2) {
	val ram = new ProgressBar(Type.SUCCESS, 0)
	val swap = new ProgressBar(Type.WARNING, 0)

	val ramRight = new TextLabel("RAM (0%) 0M")
	val swapRight = new TextLabel("SWAP (0%) 0M")

	refresh()

	put(new Row(2, ramRight.asPanel(3), ram.asPanel(9)).asPanel(12))
	put(new Row(2, swapRight.asPanel(3), swap.asPanel(9)).asPanel(12))

	def refresh() {
		ram.setProgress((MemMonProvider.total - MemMonProvider.free) / MemMonProvider.total.toDouble)
		swap.setProgress((MemMonProvider.swap - MemMonProvider.swapFree) / (MemMonProvider.swap.toDouble + 0.00001))

		ramRight.setText("RAM (" + math.floor((MemMonProvider.total - MemMonProvider.free) / MemMonProvider.total.toDouble * 100).toInt + "%) "+(MemMonProvider.total - MemMonProvider.free)/1024+"M")
		swapRight.setText("SWAP (" + math.floor((MemMonProvider.swap - MemMonProvider.swapFree) / (MemMonProvider.swap.toDouble + 0.00001) * 100).toInt + "%) "+(MemMonProvider.swap - MemMonProvider.swapFree)/1024+"M")
	}
}
