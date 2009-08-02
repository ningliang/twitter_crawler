package common;

import java.io.Serializable;
import java.util.Date;

public class Biography implements Serializable {
	private static final long serialVersionUID = 2767462670848472356L;
	public long id;
	public String name;
	public String screenName;
	public String location;
	public String description;
	public String profileImageUrl;
	public String websiteUrl;
	
	public long followersCount;
	public long friendCount;
	public long favoritesCount;
	public long statusCount;
	
	public String profileBackgroundColor;
	public String profileTextColor;
	public String profileLinkColor;
	public String profileSidebarFillColor;
	public String profileSidebarBorderColor;
	public String profileBackgroundImageUrl;
	public Boolean profileBackgroundTile;
	public Boolean verified;
	public Boolean notifications;
	
	public Date createdAt;
	public long utcOffset;
	public String timeZone;	
}
