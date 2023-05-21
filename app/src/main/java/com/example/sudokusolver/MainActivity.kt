package com.example.sudokusolver

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.gridlayout.widget.GridLayout
import com.example.sudokusolver.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity() {
    private val dimensions = 9

    @Volatile
    private var solvingJob: Job? = null

    private lateinit var binding: ActivityMainBinding
    private var gridNumbers = Array(dimensions) { Array(dimensions) { 0 } }
    private val gridTextViews = Array(dimensions) { arrayOfNulls<TextView>(dimensions) }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result?.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                val regex = Regex("""\d""")
                val text = contentResolver.openInputStream(uri)!!.reader().readText()
                regex.findAll(text).forEachIndexed { index, matchResult ->
                    gridNumbers[index / dimensions][index % dimensions] = matchResult.value.toInt()
                }
                GlobalScope.launch(Dispatchers.IO) {
                    solvingJob?.cancelAndJoin()
                    showSudoku()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityMainBinding.inflate(layoutInflater).apply {
            binding = this
            setContentView(root)
            initLayout()
            binding.btnGenerate.setOnClickListener {
                var k = et.text.toString().toIntOrNull()
                if (k == null || k < 0 || k > 81) {
                    et.setText("30")
                    k = 30
                }
                GlobalScope.launch(Dispatchers.IO) {
                    solvingJob?.cancelAndJoin()
                    gridNumbers = Sudoku.generateSudoku(k)
                    showSudoku()
                }
            }

            binding.btnSolve.setOnClickListener {
                solveSudokuStepByStep()
            }
            binding.btnStop.setOnClickListener {
                solvingJob?.cancel()
            }

            binding.btnReadFile.setOnClickListener {
                var intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent = Intent.createChooser(intent, "Choose a file")
                resultLauncher.launch(intent)
            }
        }
    }

    fun initLayout() {
        binding.gridView.removeAllViews()
        for (i in 0 until dimensions) {
            for (j in 0 until dimensions) {
                val textview = TextView(this).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.TRANSPARENT)
                }
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(i, GridLayout.FILL, 1f),
                    GridLayout.spec(j, GridLayout.FILL, 1f)
                ).apply {
                    width = 0
                    height = 0
                }

                binding.gridView.addView(textview, params)
                gridTextViews[i][j] = textview
            }
        }
    }

    fun showSudoku() {
        GlobalScope.launch(Dispatchers.Main) {
            for (i in 0 until dimensions) {
                for (j in 0 until dimensions) {
                    val textview = gridTextViews[i][j]
                    val valueInt = gridNumbers[i][j]
                    if (valueInt > 0) {
                        textview!!.text = valueInt.toString()
                    } else {
                        textview!!.text = ""
                    }
                }
            }
        }
    }

    private fun solveSudokuStepByStep() {
        if (solvingJob?.isActive != true) {
            solvingJob = GlobalScope.launch(Dispatchers.IO) {

                val gridNumbersCopy = Array(dimensions) { i ->
                    Array(dimensions) { j ->
                        gridNumbers[i][j]
                    }
                }

                if (solveSudokuDriver(gridNumbersCopy)) {
                    for (action in actions) {
                        gridNumbers[action.row][action.column] = action.number
                        val textview = gridTextViews[action.row][action.column]

                        val colorAnimation =
                            withContextNonCancellable(Dispatchers.Main) {
//                            textview.setBackgroundColor(Color.GREEN)
                                ValueAnimator.ofObject(
                                    ArgbEvaluator(),
                                    Color.TRANSPARENT,
                                    Color.GREEN
                                ).apply {
                                    duration = 500
                                    addUpdateListener { animator ->
//                                        textview.setBackgroundColor(Color.GREEN)
                                        textview!!.setBackgroundColor(animator.animatedValue as Int)
                                    }

                                    addListener(object : AnimatorListener {
                                        override fun onAnimationStart(animation: Animator) {
                                            launch(Dispatchers.Main) {
                                                println("here 1")
                                                textview!!.text = action.number.toString()
                                            }
                                        }

                                        override fun onAnimationEnd(animation: Animator) {
                                        }

                                        override fun onAnimationCancel(animation: Animator) {
                                        }

                                        override fun onAnimationRepeat(animation: Animator) {
                                        }
                                    })
                                    start()
                                }
                            }
                        val lock = ReentrantLock()
                        val condition = lock.newCondition()

                        lock.withLock {
                            colorAnimation.doOnEnd {
                                lock.withLock {
                                    condition.signalAll()
                                }
                            }
                            condition.await()
                        }

                        var running = true
                        try {
                            delay(500)
                        } catch (_: Throwable) {
                            running = false
                        } finally {
                            withContextNonCancellable(Dispatchers.Main) {
                                colorAnimation.end()
                                textview!!.setBackgroundColor(Color.TRANSPARENT)
                            }
                            if (!running) {
                                break
                            }
                        }
                    }
                } else {
                    withContextNonCancellable(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "not solvable!", Toast.LENGTH_LONG).show()
                    }
                }
                solvingJob = null
            }
        }
    }
}

suspend inline fun <T> withContextNonCancellable(
    context: CoroutineContext,
    noinline block: CoroutineScope.() -> T
): T {
    return withContext(NonCancellable) {
        withContext(context, block)
    }
}
