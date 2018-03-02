package ie.bask.niftysecondhandshop.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import ie.bask.niftysecondhandshop.R;
import ie.bask.niftysecondhandshop.models.Advert;

class AdvertAdapter extends ArrayAdapter<Advert> {
    private Context context;
    private List<Advert> adverts;
    private ViewHolder v;


    public AdvertAdapter(Context context, List<Advert> adverts) {
        super(context, R.layout.row_advert, adverts);
        this.context = context;
        this.adverts = adverts;
    }

    static class ViewHolder {
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        TextView productLocation;
        TextView productDescription;
    }

    @Override
    public Advert getItem(int position) {
        return adverts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.row_advert, parent, false);
        } else {
            view = convertView;
        }

        v = new ViewHolder();
        v.productImage = view.findViewById(R.id.row_image);
        v.productTitle = view.findViewById(R.id.row_title);
        v.productPrice = view.findViewById(R.id.row_price);
        v.productLocation = view.findViewById(R.id.row_location);
        v.productDescription = view.findViewById(R.id.row_details);

        final Advert dataSet = adverts.get(position);
        v.productImage.setImageURI(dataSet.getImageUri());
        v.productTitle.setText(dataSet.getProductTitle());
        v.productPrice.setText("€" + dataSet.getProductPrice());
        v.productLocation.setText(dataSet.getProductLocation());
        v.productDescription.setText(dataSet.getProductDescription());

        return view;
    }

    @Override
    public int getCount() {
        return adverts.size();
    }

}