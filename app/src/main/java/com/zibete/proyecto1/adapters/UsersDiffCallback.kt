import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_AGE
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_DESCRIPTION
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_DISTANCE_METERS
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_NAME
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_ONLINE
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_PHOTO_URL

object UsersDiffCallback : DiffUtil.ItemCallback<Users>() {

    override fun areItemsTheSame(oldItem: Users, newItem: Users): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Users, newItem: Users): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Users, newItem: Users): Any? {
        val diff = Bundle()

        if (oldItem.distanceMeters != newItem.distanceMeters) diff.putBoolean(PAYLOAD_DISTANCE_METERS, true)
        if (oldItem.name != newItem.name) diff.putBoolean(PAYLOAD_NAME, true)
        if (oldItem.age != newItem.age) diff.putBoolean(PAYLOAD_AGE, true)
        if (oldItem.description != newItem.description) diff.putBoolean(PAYLOAD_DESCRIPTION, true)
        if (oldItem.profilePhoto != newItem.profilePhoto) diff.putBoolean(PAYLOAD_PHOTO_URL, true)
        if (oldItem.isOnline != newItem.isOnline) diff.putBoolean(PAYLOAD_ONLINE, newItem.isOnline)

        return if (diff.isEmpty) null else diff
    }
}
