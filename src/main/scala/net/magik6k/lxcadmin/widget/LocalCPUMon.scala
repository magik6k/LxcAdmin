package net.magik6k.lxcadmin.widget

import net.magik6k.jwwf.widgets.basic.{ProgressBar, TextLabel}
import net.magik6k.jwwf.widgets.basic.panel.Row
import net.magik6k.lxcadmin.util.Using

import scala.io.Source

object CPUMonProvider {
	val cores = Runtime.getRuntime.availableProcessors()
	val loads: Array[Float] = new Array[Float](cores)
	val prevIdle: Array[Long] = new Array[Long](cores)
	val prevTotal: Array[Long] = new Array[Long](cores)

	Using(Source.fromFile("/proc/stat")){_.getLines().map(line => line.split("\\s+"))
		.filter(cpu => cpu(0).startsWith("cpu") && !cpu(0).equals("cpu"))
		.zipWithIndex.foreach{
		case(cpu, id) =>
			prevIdle(id) = cpu(4).toLong
			prevTotal(id) = cpu(1).toLong + cpu(2).toLong + cpu(3).toLong + cpu(4).toLong
			loads(id) = 0
	}}

	def refresh() = {
		Using(Source.fromFile("/proc/stat")){_.getLines().map(line => line.split("\\s+"))
			.filter(cpu => cpu(0).startsWith("cpu") && !cpu(0).equals("cpu"))
			.zipWithIndex.foreach{case(cpu, id) =>
			val idle = cpu(4).toLong
			val total = cpu(1).toLong + cpu(2).toLong + cpu(3).toLong + cpu(4).toLong
			val deltaTotal: Float = total - prevTotal(id)

			loads(id) = (deltaTotal - (idle - prevIdle(id))) / deltaTotal

			prevIdle(id) = idle
			prevTotal(id) = total
		}}
	}

}

class LocalCPUMon extends Row(Runtime.getRuntime.availableProcessors()){
	val bars = new Array[ProgressBar](Runtime.getRuntime.availableProcessors())

	CPUMonProvider.loads.zipWithIndex.foreach{case (load, id) => {bars(id) = new ProgressBar(load); put(bars(id))}}

	def refresh() {
		CPUMonProvider.loads.zipWithIndex.foreach{case (load, id) => {bars(id).setProgress(load)}}
	}
}

