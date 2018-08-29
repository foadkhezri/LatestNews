package foadkhezri.github.com.latestnews;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends ArrayAdapter {

    public NewsAdapter(@NonNull NewsActivity context, ArrayList<News> news) {
        super(context, 0, news);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        News currentNews = (News) getItem(position);

        View listItemView = convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }
        assert currentNews != null;
        TextView title = listItemView.findViewById(R.id.title);
        TextView date = listItemView.findViewById(R.id.date);
        title.setText(currentNews.getTitle());
        date.setText(currentNews.getDate());
        return listItemView;
    }
}
