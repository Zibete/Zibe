package com.zibete.proyecto1.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.zibete.proyecto1.R;

public class SnackUtils {

    public static void show(
            View root,
            String message,
            int duration,
            @ColorRes int bgColor,
            @Nullable String actionText,
            @Nullable View.OnClickListener action,
            @DrawableRes int iconRes
    ) {
        Snackbar snackbar = Snackbar.make(root, "", duration);

        // Inflamos nuestro layout
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundColor(0x00000000); // transparente para que se vea el card

        View customView = LayoutInflater.from(root.getContext())
                .inflate(R.layout.layout_snackbar_zibe, null, false);

        // Texto
        TextView tv = customView.findViewById(R.id.snack_text);
        tv.setText(message);

        // Icono opcional
        ImageView iv = customView.findViewById(R.id.snack_icon);
        if (iconRes != 0) {
            iv.setVisibility(View.VISIBLE);
            iv.setImageResource(iconRes);
        } else {
            iv.setVisibility(View.GONE);
        }

        // Acción opcional
        TextView tvAction = customView.findViewById(R.id.snack_action);
        if (actionText != null && action != null) {
            tvAction.setVisibility(View.VISIBLE);
            tvAction.setText(actionText);
            tvAction.setOnClickListener(v -> {
                action.onClick(v);
                snackbar.dismiss();
            });
        }

        // Color de fondo
        customView.setBackgroundTintList(
                root.getResources().getColorStateList(bgColor)
        );

        // Limpiamos views anteriores del layout del snackbar
        layout.removeAllViews();
        layout.addView(customView);

        snackbar.show();
    }

    // Overload rápido para “mensaje + OK”
    public static void showInfo(View root, String message) {
        show(
                root,
                message,
                Snackbar.LENGTH_SHORT,
                R.color.colorC,      // tu color
                "OK",
                v -> {},
                R.drawable.ic_info_24
        );
    }
}
