package com.example.rxjava2

import io.reactivex.Flowable
import io.reactivex.FlowableSubscriber
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscription
import java.util.concurrent.Semaphore

fun main(args: Array<String>) {
    val semaphore = Semaphore(0)
    val nonRx = object {
        val worker = Schedulers.computation().createWorker()
        val arr: List<Int> = (1..20).map { it }
        var i: Long = 0
        lateinit var listener: (Int) -> Unit

        fun request(num: Long) = worker.schedule {
            if (i < arr.size) i = (i + num).also {
                (i until arr.size.toLong().coerceAtMost(it))
                        .map(Long::toInt)
                        .forEach { arr[it].let(listener) }
            } else semaphore.release()
        }.let {}

        fun cancel() = Unit
    }

    Flowable.fromPublisher<Int> {
        nonRx.listener = it::onNext
        it.onSubscribe(object : Subscription {
            override fun cancel() {
                nonRx.cancel()
            }

            override fun request(n: Long) {
                nonRx.request(n)
            }

        })
    }.subscribe(object : FlowableSubscriber<Int> {
        lateinit var s: Subscription
        override fun onComplete() {
            println("onComplete")
        }

        override fun onNext(t: Int?) {
            println("onNext($t)")
            Thread.sleep(1000)
            s.request(1)
        }

        override fun onSubscribe(s: Subscription) {
            println("onSubscribe")
            this.s = s
            s.request(1)
        }

        override fun onError(t: Throwable?) {
            t?.printStackTrace()
        }
    })
    semaphore.acquire()
}
