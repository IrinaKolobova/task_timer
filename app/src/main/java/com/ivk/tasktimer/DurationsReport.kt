package com.ivk.tasktimer

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.task_durations.*
import java.lang.IllegalArgumentException

private const val TAG = "DurationsReport"

private const val DIALOG_FILTER = 1
private const val DIALOG_DELETE = 2

class DurationsReport : AppCompatActivity(),
    DatePickerDialog.OnDateSetListener,
    View.OnClickListener {

    private val viewModel by lazy { ViewModelProviders.of(this).get(DurationsViewModel::class.java) }

    private val reportAdapter by lazy { DurationsRVAdapter(this, null) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: starts")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_durations_report)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        td_list.layoutManager = LinearLayoutManager(this)
        td_list.adapter = reportAdapter

        viewModel.cursor.observe(this, Observer { cursor -> reportAdapter.swapCursor(cursor)?.close() })

        // Set the listener for the buttons so we can sort the report.
        td_name_heading.setOnClickListener(this)
        td_description_heading?.setOnClickListener(this)    // description will not be present in portrait
        td_start_heading.setOnClickListener(this)
        td_duration_heading.setOnClickListener(this)
        Log.d(TAG, "onCreate: finished")
    }

    override fun onClick(v: View) {
        Log.d(TAG, "onClick: called")
        when (v.id) {
            R.id.td_name_heading -> viewModel.sortOrder = SortColumns.NAME
            R.id.td_description_heading -> viewModel.sortOrder = SortColumns.DESCRIPTION
            R.id.td_start_heading -> viewModel.sortOrder = SortColumns.START_DATE
            R.id.td_duration_heading -> viewModel.sortOrder = SortColumns.DURATION
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "onCreateOptionsMenu: called")
        menuInflater.inflate(R.menu.menu_report, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: called")
        val id = item.itemId
        when (id) {
            R.id.rm_filter_period -> {
                viewModel.toggleDisplayWeek() // was showing a week, so now show a day - or vice versa
                invalidateOptionsMenu() // force cal to onPrepareOptionsMenu to redraw our changed menu
                return true
            }
            R.id.rm_filter_date -> {
                showDatePickerDialog(getString(R.string.date_title_filter), DIALOG_FILTER)
                return true
            }
            R.id.rm_delete -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onPrepareOptionsMenu: called")
        val item = menu.findItem(R.id.rm_filter_period)
        if (item != null) {
            // switch icon and title to represent 7 days or 1 day, as appropriate to the future function of the menu item.
            if (viewModel.displayWeek) {
                item.setIcon(R.drawable.ic_baseline_filter_1_24)
                item.setTitle(R.string.rm_title_filter_day)
            } else {
                item.setIcon(R.drawable.ic_baseline_filter_7_24)
                item.setTitle(R.string.rm_title_filter_week)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun showDatePickerDialog(title: String, dialogId: Int) {
        val dialogFragment = DatePickerFragment()

        val arguments = Bundle()
        arguments.putInt(DATE_PICKER_ID, dialogId)
        arguments.putString(DATE_PICKER_TITLE, title)
        arguments.putSerializable(DATE_PICKER_DATE, viewModel.getFilterDate())
        dialogFragment.arguments = arguments
        dialogFragment.show(supportFragmentManager, "datePicker")
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        Log.d(TAG, "onDateSet: called")

        // Check the id, so we know what to do with the result
        val dialogId = view.tag as Int
        when (dialogId) {
            DIALOG_FILTER -> {
                viewModel.setReportDate(year,month, dayOfMonth)
            }
            DIALOG_DELETE -> {}
            else -> throw IllegalArgumentException("Invalid mode when receiving DatePickerDialog result")
        }
    }

    //    override fun onDestroy() {
//        reportAdapter.swapCursor(null)?.close()
//        super.onDestroy()
//    }
}