import csv
from collections import defaultdict
import math
import os
import sys
import random
import time
import tracemalloc
import functools

# Define global min and max ratings
GLOBAL_MIN_RATING = 1.0
GLOBAL_MAX_RATING = 5.0

# ------------------------ TIME & MEMORY USAGE MONITORING ------------------------

def performance_monitor(func):
    """
    Decorator to measure both execution time and memory usage of a function.
    """
    @functools.wraps(func)
    def wrapper_performance_monitor(*args, **kwargs):
        # Start memory tracking
        tracemalloc.start()
        snapshot1 = tracemalloc.take_snapshot()
        
        # Start timing
        start_time = time.perf_counter()
        
        try:
            # Execute the function
            result = func(*args, **kwargs)
        except Exception as e:
            tracemalloc.stop()
            print(f"\n    [Error] Function '{func.__name__}' raised an exception: {e}")
            raise
        
        # End timing
        end_time = time.perf_counter()
        elapsed_time = end_time - start_time
        
        # End memory tracking
        snapshot2 = tracemalloc.take_snapshot()
        tracemalloc.stop()
        
        # Calculate memory usage
        top_stats = snapshot2.compare_to(snapshot1, 'lineno')
        
        # Display results
        print(f"\n    [Performance] Function '{func.__name__}' executed in {elapsed_time:.4f} seconds.")
        print(f"    [Performance] Memory usage in '{func.__name__}':")
        
        # Initialize list to store sizes of top 3 memory-consuming lines
        sizes = []
        
        # Print top 3 memory-consuming lines and collect their sizes
        for stat in top_stats[:3]:  # Show top 3 memory-consuming lines
            print("        ", stat)
            sizes.append(stat.size)
        
        # Calculate the sum of the top 3 sizes
        total_size = sum(sizes)
        # Create a string that joins the sizes with a plus sign
        sizes_sum = " + ".join(map(str, sizes))
        
        # Print the sum of the top 3 memory usages
        print(f"         Approx Memory Usage: {sizes_sum} = {total_size} B")
        print()
        return result
    return wrapper_performance_monitor

# ------------------------ END OF TIME & MEMORY USAGE MONITORING ------------------------


# ------------------------ USER vs. SYSTEM INTERACTIONS ------------------------

def get_metric():
    metrics = ["cosine", "jaccard"]
    while True:
        metric_name = input("    B. Enter the metric which you would like to use: ").strip().lower()
        if metric_name in metrics:
            metric = {
                "cosine": "Cosine Similarity",
                "jaccard": "Jaccard Similarity"
            }[metric_name]
            print(f"       You have selected the metric: {metric}")
            return metric_name, metric
        else:
            print("       Metric not found. Please try again.")

def get_eval_metric():
    metrics = ["mae", "rsme", "precision", "recall", "all"]
    print("    We provide 4 possible evaluation metrics to assess the prediction quality.\n")
    print("        1. Mean Absolute Error (Input: mae)")
    print("        2. Root Mean Square Error (Input: rsme)")
    print("        3. Precision (Input: precision)")
    print("        4. Recall (Input: recall)")
    print("        5. MAE, RSME, Precision and Recall (Input: all)")
    while True:
        metric_name = input("\n    D. Enter the evaluation metric which you would like to use: ").strip().lower()
        if metric_name in metrics:
            metric = {
                "mae": "Mean Absolute Error",
                "rsme": "Root Mean Square Error",
                "precision": "Precision",
                "recall": "Recall",
                "all": "Mean Absolute Error, Root Mean Square Errors, Precision and Recall"
            }[metric_name]
            print(f"       You have selected the evaluation metric: {metric}")
            return metric_name
        else:
            print("       Metric not found. Please try again.")

def get_top_n():
    print("       The metrics Precision and Recall will be determined based on a given top number of items.")
    while True:
        top_n_input = input("\n    E. How many elements would you like to consider? ").strip()
        # If the input is empty, the default value is 3
        if not top_n_input:
            return 3

        # Try converting the input to a int
        try:
            top_n = int(top_n_input)
            if 2 <= top_n <= 5:
                return top_n
            else:
                print("       The number must be between 2 and 5. Please try again.")
        except ValueError:
            print("       Invalid input. Please enter a numerical value for the number.")

def get_similarity_threshold():
    while True:
        similarity_threshold_input = input("    C. Enter the similarity threshold (leave empty for default 0.05): ")

        # If the input is empty, use the default value
        if not similarity_threshold_input:
            print() # Skipping a line for esthetics in terminal
            return 0.05

        # Try converting the input to a float
        try:
            similarity_threshold = float(similarity_threshold_input)
            if 0 <= similarity_threshold <= 1:
                print() # Skipping a line for esthetics in terminal
                return similarity_threshold
            else:
                print("       Threshold must be between 0 and 1. Please try again.")
        except ValueError:
            print("       Invalid input. Please enter a numerical value for the threshold.")

def get_nb_users_precision_recall(precision_recall, metric_type='precision', max_users=5):
    """
    Prompts the user to select up to `max_users` users to view their precision or recall.

    Parameters:
    - precision_recall (dict): Precision and recall {user_id: {'precision': value, 'recall': value}}.
    - metric_type (str): Type of metric to view ('precision', 'recall', or 'both').
    - max_users (int): Maximum number of users to select.

    Returns:
    - selected_users (list): List of selected user IDs.
    """
    selected_users = []
    print(f"\n    G. You can select up to {max_users} users to view their {metric_type}.")
    while len(selected_users) < max_users:
        user_input = input(f"          Enter User ID ({len(selected_users)+1}/{max_users}, leave empty to finish): ").strip()
        if not user_input:
            break
        try:
            user_id = int(user_input)
            if user_id in precision_recall:
                if user_id not in selected_users:
                    selected_users.append(user_id)
                else:
                    print("          User already selected. Choose a different user.")
            else:
                print("          User ID not found. Please enter a valid User ID.")
        except ValueError:
            print("          Invalid input. Please enter a numerical User ID.")
    
    return selected_users

def get_similarity(user_similarity, user1, user2):
    return user_similarity.get(user1, {}).get(user2, 0.0)

def get_top_n_actual(all_ratings, top_n=5):
    """
    Extracts the top N rated movies per user from the original dataset.

    Parameters:
    - all_ratings (list): List of all ratings as tuples (user_id, movie_id, rating).
    - top_n (int): Number of top movies to extract per user.

    Returns:
    - actual_top_n (dict): Actual top N movies {user_id: [movie_id, ...]}.
    """
    user_actual_ratings = defaultdict(list)
    for user_id, movie_id, rating in all_ratings:
        user_actual_ratings[user_id].append((movie_id, rating))
    
    # Sort the movies for each user by rating in descending order and take top N
    actual_top_n = {}
    for user, movies in user_actual_ratings.items():
        sorted_movies = sorted(movies, key=lambda x: x[1], reverse=True)
        top_movies = [movie for movie, rating in sorted_movies[:top_n]]
        actual_top_n[user] = top_movies
    
    return actual_top_n

# ------------------------ END OF USER vs. SYSTEM INTERACTIONS ------------------------


# ------------------------ NORMALIZATION & DENORMALIZATION ------------------------

def normalize(rating):
    return (rating - GLOBAL_MIN_RATING) / (GLOBAL_MAX_RATING - GLOBAL_MIN_RATING)

def denormalize(normalized_rating):
    """
    Converts a normalized rating back to its original scale.

    Parameters:
    - normalized_rating (float): Normalized rating.

    Returns:
    - original_rating (float): Original rating.
    """
    return normalized_rating * (GLOBAL_MAX_RATING - GLOBAL_MIN_RATING) + GLOBAL_MIN_RATING

# ------------------------ END OF NORMALIZATION & DENORMALIZATION ------------------------



# ------------------------ TESTING AND DEBUGGING STRATEGIES ------------------------

def print_progress_bar(iteration, total, function_name, length=50):
    percent = f"{100 * (iteration / float(total)):.1f}"
    filled_length = int(length * iteration // total)
    bar = "█" * filled_length + "-" * (length - filled_length)

    # Displaying the progress bar with the function name
    sys.stdout.write(f"\r    [{function_name}] Progress: |{bar}| {percent}% Complete")
    sys.stdout.flush()

def print_precision_selected(precision_recall, selected_users):
    """Print precision for selected users."""
    print("\n       ————————— Precision —————————")
    for user in selected_users:
        precision = precision_recall[user]['precision']
        print(f"          User {user} - Precision: {precision:.2f}")

def print_recall_selected(precision_recall, selected_users):
    """Print recall for selected users."""
    print("\n       ————————— Recall —————————")
    for user in selected_users:
        recall = precision_recall[user]['recall']
        print(f"          User {user} - Recall: {recall:.2f}")

# ------------------------ END OF TESTING AND DEBUGGING STRATEGIES ------------------------




# ------------------------  DATA LOADING & TRAIN/TEST SPLITTING  ------------------------

@performance_monitor
def split_train_test(filename, test_ratio=0.2, min_users=2, seed=42, min_common_movies=2):
    """
    Splits the dataset into training and testing sets based on specified criteria.

    Parameters:
    - filename (str): Path to the CSV ratings file.
    - test_ratio (float): Proportion of eligible ratings to assign to the test set.
    - min_users (int): Minimum number of other users who must have rated the same movie.
    - seed (int): Random seed for reproducibility.
    - min_common_movies (int): Minimum number of common movies with other users.

    Returns:
    - train_ratings (dict): Training dataset {user_id: {movie_id: normalized_rating}}.
    - test_ratings (dict): Testing dataset {user_id: {movie_id: normalized_rating}}.
    - all_ratings (list): Complete list of all ratings as tuples (user_id, movie_id, rating).
    """
    random.seed(seed)  # For reproducibility

    # Step 1: Loading all ratings and building the mappings dictionaries
    all_ratings = []
    movie_to_users = defaultdict(set)
    user_to_movies = defaultdict(set)

    with open(filename, 'r') as file:
        next(file)  # Skip header
        reader = csv.reader(file, delimiter=',')
        for row in reader:
            user_id, movie_id, rating, _ = row
            user_id = int(user_id)
            movie_id = int(movie_id)
            rating = float(rating)
            all_ratings.append((user_id, movie_id, rating))
            movie_to_users[movie_id].add(user_id)
            user_to_movies[user_id].add(movie_id)

    # Step 2: Identifying eligible ratings for the test set based on both defined criteria (report)
    eligible_ratings = []
    total_ratings = len(all_ratings)
    print(f"\n    [split_train_test] Identifying eligible ratings for the test set...")
    for idx, rating in enumerate(all_ratings, 1):
        user_id, movie_id, _ = rating
        other_users = movie_to_users[movie_id] - {user_id}

        # Checking if the movie has at least 'min_users' other users
        if len(other_users) >= min_users:
            # Counting how many other users have at least 'min_common_movies' in common with the current user
            count = 0
            for other_user in other_users:
                # Excluding the current movie from the common movies
                common_movies = user_to_movies[user_id].intersection(user_to_movies[other_user]) - {movie_id}
                if len(common_movies) >= min_common_movies:
                    count += 1

            # If at least 'min_users' other users meet the common movies criterion, include the rating
            if count >= min_users:
                eligible_ratings.append(rating)

        # Updating progress bar every 100 iterations to reduce console clutter
        if idx % 100 == 0 or idx == total_ratings:
            print_progress_bar(idx, total_ratings, "Splitting Data")

    print()  # Moving to the next line after progress bar completes

    # Step 3: Shuffling eligible ratings
    random.shuffle(eligible_ratings)

    # Step 4: Computing number of test ratings
    num_test = int(len(eligible_ratings) * test_ratio)

    # Step 5: Selecting the test ratings
    test_ratings_list = eligible_ratings[:num_test]
    train_ratings_list = [r for r in all_ratings if r not in test_ratings_list]

    # Step 6: Converting lists to dictionaries with normalization
    train_ratings = defaultdict(dict)
    for user_id, movie_id, rating in train_ratings_list:
        clipped_rating = max(GLOBAL_MIN_RATING, min(rating, GLOBAL_MAX_RATING))
        normalized = normalize(clipped_rating)
        train_ratings[user_id][movie_id] = normalized

    test_ratings = defaultdict(dict)
    for user_id, movie_id, rating in test_ratings_list:
        clipped_rating = max(GLOBAL_MIN_RATING, min(rating, GLOBAL_MAX_RATING))
        normalized = normalize(clipped_rating)
        test_ratings[user_id][movie_id] = normalized

    return train_ratings, test_ratings, all_ratings


# ------------------------  END OF DATA LOADING & TRAIN/TEST SPLITTING  ------------------------



# ------------------------ SIMILARITY METRICS, COMPUTATIONS, PREDICTIONS AND RECOMMENDATIONS ------------------------

def cosine_similarity(ratings1, ratings2):
    # Calculating the dot product of the two rating lists
    dot_product = sum([r1 * r2 for r1, r2 in zip(ratings1, ratings2)])
    
    # Calculating the norms (magnitudes) of each rating list
    norm1 = math.sqrt(sum([r1 ** 2 for r1 in ratings1]))
    norm2 = math.sqrt(sum([r2 ** 2 for r2 in ratings2]))
    
    # Returning the cosine similarity, handling the case where norms are zero
    return dot_product / (norm1 * norm2) if norm1 and norm2 else 0.0

def jaccard_similarity(vec1, vec2):
    # Converting lists to sets
    set1 = set(vec1)
    set2 = set(vec2)
    
    # Calculating the intersection and union of the sets
    intersection = len(set1 & set2)
    union = len(set1 | set2)
    
    # Returning the Jaccard similarity, avoiding division by zero
    return intersection / union if union != 0 else 0.0

@performance_monitor
def compute_similarity(user_ratings, similarity_type='cosine', min_common=2):
    """
    Computes similarity between users based on specified similarity type.

    Parameters:
    - user_ratings (dict): User ratings {user_id: {movie_id: normalized_rating}}.
    - similarity_type (str): Type of similarity ('cosine', 'jaccard', etc.).
    - min_common (int): Minimum number of common movies required to compute similarity.

    Returns:
    - user_similarity (dict): User similarity scores {user1: {user2: similarity, ...}, ...}.
    """
    # We add a new parameter to improve the similarity accuracy as 
    # with just one possible common element it yields 1.0
    # That's why we add the min_common elements
    # Only user pairs with at least 2 common movies will have
    # a non-zero similarity, leading to more reliable similarity scores
    
    # Creating empty dictionary
    user_similarity = defaultdict(dict)

    # Calculating total number of comparisons (pairs of users) for the progress bar
    n = len(user_ratings)
    total_comparisons = n * (n - 1) // 2  # Integer division for whole number
    current_progress = 0

    # Computing similarity for each pair of users
    users = list(user_ratings.keys())
    for i in range(len(users)):
        user1 = users[i]
        for j in range(i + 1, len(users)):
            user2 = users[j]

            # Getting the common movies rated by both users
            common_movies = set(user_ratings[user1]).intersection(user_ratings[user2])
            if len(common_movies) < min_common:
                similarity = 0.0
            else:
                ratings1 = [user_ratings[user1][movie] for movie in common_movies]
                ratings2 = [user_ratings[user2][movie] for movie in common_movies]
                
                # Debugging output
                #print(f"\nCalculating similarity between User {user1} and User {user2} on movies {common_movies}")
                #print(f"Ratings1: {ratings1}, Ratings2: {ratings2}")
                
                # Applying similarity based on the specified type
                if similarity_type == 'cosine':
                    similarity = cosine_similarity(ratings1, ratings2)
                elif similarity_type == 'jaccard':
                    similarity = jaccard_similarity(ratings1, ratings2)
                else:
                    similarity = 0.0

            # Storing the similarity score in both directions
            user_similarity[user1][user2] = similarity
            user_similarity[user2][user1] = similarity
            # Updating progress bar
            current_progress += 1
            print_progress_bar(current_progress, total_comparisons, "Computing Similarity")

    # Printing a new line after the progress bar is complete
    print("\n")
    return user_similarity

@performance_monitor
def predict_ratings(user_ratings, user_similarity, similarity_threshold):
    """
    Predicts ratings for unrated movies based on user similarities.

    Parameters:
    - user_ratings (dict): Training dataset {user_id: {movie_id: normalized_rating}}.
    - user_similarity (dict): User similarity scores {user1: {user2: similarity, ...}, ...}.
    - similarity_threshold (float): Minimum similarity required to consider a user.

    Returns:
    - predicted_ratings (dict): Predicted ratings {user_id: {movie_id: predicted_normalized_rating}}.
    """

    predicted_ratings = defaultdict(dict)

    # Calculating total number of users and movies to predict for the progress bar
    total_predictions = sum(
        len({movie for other_user in user_ratings for movie in user_ratings[other_user] if movie not in user_ratings[user]})
        for user in user_ratings
    )
    current_progress = 0

    for user in user_ratings:
        # Movies that this user hasn't rated
        unrated_movies = {movie for other_user in user_ratings for movie in user_ratings[other_user] if movie not in user_ratings[user]}

        for movie in unrated_movies:
            # Finding similar users
            similar_users = []
            for other_user in user_ratings:
                if other_user == user:
                    continue
                sim = get_similarity(user_similarity, user, other_user)
                if sim >= similarity_threshold and movie in user_ratings[other_user]:
                    similar_users.append((sim, other_user))
            
            if similar_users:
                numerator = sum(sim * user_ratings[other_user][movie] for sim, other_user in similar_users)
                denominator = sum(sim for sim, _ in similar_users)
                predicted_rating = numerator / denominator if denominator != 0 else 0
            else:
                predicted_rating = 0.0

            # Clipping the predicted rating to the range [0, 1] because we normalized ratings beforehand
            predicted_rating = min(max(predicted_rating, 0), 1)
            predicted_ratings[user][movie] = predicted_rating

            # Updating progress bar
            current_progress += 1
            print_progress_bar(current_progress, total_predictions, "Predicting Ratings")

    # Printing a new line after the progress bar is complete
    print("\n")
    return predicted_ratings

def recommend_movies(train_ratings, predicted_ratings, top_n=5):
    """
    Generates top N movie recommendations for each user by combining actual and predicted ratings.

    Parameters:
    - train_ratings (dict): Training dataset {user_id: {movie_id: normalized_rating}}.
    - predicted_ratings (dict): Predicted ratings {user_id: {movie_id: normalized_rating}}.
    - top_n (int): Number of top recommendations to generate.

    Returns:
    - recommendations (dict): Recommendations {user_id: [(movie_id, rating), ...]}.
    """
    recommendations = {}

    for user in train_ratings.keys():
        # Getting the rated movies with actual ratings
        rated_movies = train_ratings[user]

        # Getting predicted ratings
        predicted = predicted_ratings.get(user, {})

        # Combining both into a list of tuples (movie_id, rating)
        combined = list(rated_movies.items()) + list(predicted.items())

        # Sorting by rating descending
        sorted_combined = sorted(combined, key=lambda x: x[1], reverse=True)

        # Selecting top N movies
        top_movies = sorted_combined[:top_n]

        # Storing recommendations
        recommendations[user] = top_movies

    return recommendations

# ------------------------ END OF SIMILARITY METRICS, COMPUTATIONS, PREDICTIONS AND RECOMMENDATIONS ------------------------


# ------------------------ MAE, RSME, PRECISION AND RECALL COMPUTATIONS ------------------------

def calculate_mae_rmse(test_ratings, predicted_ratings):
    total_error_mae = 0
    total_error_rmse = 0
    total_count = 0

    for user_id, movies in test_ratings.items():
        for movie_id, actual_norm in movies.items():
            actual = denormalize(actual_norm)
            predicted_norm = predicted_ratings.get(user_id, {}).get(movie_id, 0.0)
            predicted = denormalize(predicted_norm)
            error = abs(actual - predicted)
            total_error_mae += error
            total_error_rmse += error ** 2
            total_count += 1

    mae = total_error_mae / total_count if total_count != 0 else 0.0
    rmse = math.sqrt(total_error_rmse / total_count) if total_count != 0 else 0.0
    return mae, rmse

@performance_monitor
def calculate_precision_recall(actual_top_n, recommended_top_n):
    """
    Calculates precision and recall for each user based on top N actual and recommended movies,
    displaying a progress bar during computation.

    Parameters:
    - actual_top_n (dict): Actual top N movies {user_id: [movie_id, ...]}.
    - recommended_top_n (dict): Recommended top N movies {user_id: [movie_id, ...]}.

    Returns:
    - precision_recall (dict): Precision and recall {user_id: {'precision': value, 'recall': value}}.
    """
    precision_recall = {}
    total_users = len(actual_top_n)
    current_iteration = 0

    for user, actual_movies in actual_top_n.items():
        recommended_movies = recommended_top_n.get(user, [])

        # Calculating true positives: movies that are both in actual and recommended top N
        true_positives = len(set(actual_movies) & set(recommended_movies))

        # Calculating precision and recall, avoiding division by zero
        precision = true_positives / len(recommended_movies) if recommended_movies else 0
        recall = true_positives / len(actual_movies) if actual_movies else 0

        # Storing the precision and recall for the current user
        precision_recall[user] = {
            'precision': precision,
            'recall': recall
        }

        # Updating progress bar
        current_iteration += 1
        print_progress_bar(current_iteration, total_users, "Calculating Precision and Recall")

    # Moving to the next line after the progress bar completes - esthetics
    print()

    return precision_recall

# ------------------------ END OF MAE, RSME, PRECISION AND RECALL COMPUTATIONS ------------------------


# ------------------------ MAIN EXECUTION FUNCTION ------------------------

def main():
    # Prompting the user for input
    print("\n\n ---————————— Welcome to the Movie's Recommendation System designed by Pablo Mollá —————————---")
    print("\n    The Recommendation System will follow a User-User Collaboration Filtering solution.")
    print("    We provide 2 possible metrics to determine the similarity between the movies.\n\n    1. Cosine Similarity (Input: cosine).\n    2. Jaccard Similarity (Input: jaccard).\n\n    Please, answer the following questions.")
   
    # Getting the dataset file path
    ratings_file = input("\n    A. Enter the path for the ratings file: ").strip()
    if not os.path.exists(ratings_file):
        print("    File not found. Please ensure the path is correct.\n")
        return
    
    # Getting the metric name
    metric_name, metric = get_metric()
    
    # Getting the similarity threshold
    similarity_threshold = get_similarity_threshold()

    # Splitting the dataset into training and testing
    train_ratings, test_ratings, all_ratings = split_train_test(
        ratings_file,
        test_ratio=0.2,
        min_users=2,
        seed=42,
        min_common_movies=2  # This ensures at least 2 common movies
    )

    #print("    Similarities will be computed based on the following Training Ratings:")
    #print("    Training Ratings:", train_ratings)
    #print("\n    Predictions will be done with the following Testing Ratings:")
    #print("    Testing Ratings:", test_ratings)
    #print("\n")

    # Computing user similarities with the chosen metric and minimum common movies
    user_similarity = compute_similarity(train_ratings, similarity_type=metric_name, min_common=2)
    #print("\nSIMILARITY:", user_similarity)

    # Predicting ratings based on user-user collaboration
    predicted_ratings = predict_ratings(train_ratings, user_similarity, similarity_threshold)
    #print("\nPredicted Ratings:")
    #for user, movies in predicted_ratings.items():
        #print(f"User {user}: {movies}")

    print("    Computation of Similarities and Predictions finished.\n")

    # Asking user which evaluation metric would like to consider.
    eval_metric = get_eval_metric()
    mae, rmse = calculate_mae_rmse(test_ratings, predicted_ratings)
    if eval_metric == "mae":
        print(f"       Mean Absolute Error (MAE): {mae:.4f}")
    elif eval_metric == "rsme":
        print(f"       Root Mean Squared Error (RMSE): {rmse:.4f}")
    elif eval_metric == "precision":
        top_n = get_top_n()
        print()
        # Extract actual top n movies per user from the original dataset
        actual_top_n = get_top_n_actual(all_ratings, top_n=top_n)
        
        
        # Ask user how many users' data to print
        while True:
            try:
                nb_users_to_print = int(input(f"    G. How many users top {top_n} movies would you like to see? (1-5): ").strip())
                if 1 <= nb_users_to_print <= 5:
                    break
                else:
                    print("       Please enter a number between 1 and 5.")
            except ValueError:
                print("       Invalid input. Please enter a numerical value.")
        
        # Print actual_top_n for the first nb_users_to_print users
        print(f"\n       Actual Top {top_n} Movies for {nb_users_to_print} Users:")
        for idx, (user, movies) in enumerate(actual_top_n.items()):
            if idx >= nb_users_to_print:
                break
            print(f"          User {user}: {movies}")


        # Generating Recommendations by combining train and predicted ratings
        recommendations = recommend_movies(train_ratings, predicted_ratings, top_n=top_n)
        recommended_top_n = {user: [movie for movie, rating in movies] for user, movies in recommendations.items()}
        
        # Print recommended_top_n for the first nb_users_to_print users
        print(f"\n       Recommendations (Top {top_n}) for {nb_users_to_print} Users:")
        for idx, (user, movies) in enumerate(recommended_top_n.items()):
            if idx >= nb_users_to_print:
                break
            print(f"          User {user}: {movies}")

        print()
        # Calculating Precision and Recall based on top n
        precision_recall = calculate_precision_recall(actual_top_n, recommended_top_n)

        # Asking user to select up to 5 users to view precision
        selected_users = get_nb_users_precision_recall(precision_recall, metric_type='precision', max_users=5)
        
        # Printing precision for selected users
        if selected_users:
            print_precision_selected(precision_recall, selected_users)
        else:
            print("       No users selected for precision display.")
        
        # Calculating and print average precision for all users
        total_precision = sum(metrics['precision'] for metrics in precision_recall.values())
        avg_precision = total_precision / len(precision_recall) if precision_recall else 0.0
        print(f"\n       Average Precision across all users: {avg_precision:.2f}")

    elif eval_metric == "recall":
        top_n = get_top_n()
        print()
        # Extracting actual top n movies per user from the original dataset
        actual_top_n = get_top_n_actual(all_ratings, top_n=top_n)
        
        
        # Asking user how many users' data to print
        while True:
            try:
                nb_users_to_print = int(input(f"    G. How many users top {top_n} movies would you like to see? (1-5): ").strip())
                if 1 <= nb_users_to_print <= 5:
                    break
                else:
                    print("       Please enter a number between 1 and 5.")
            except ValueError:
                print("       Invalid input. Please enter a numerical value.")
        
        # Printing actual_top_n for the first nb_users_to_print users
        print(f"\n       Actual Top {top_n} Movies for {nb_users_to_print} Users:")
        for idx, (user, movies) in enumerate(actual_top_n.items()):
            if idx >= nb_users_to_print:
                break
            print(f"          User {user}: {movies}")

        # Generating Recommendations by combining train and predicted ratings
        recommendations = recommend_movies(train_ratings, predicted_ratings, top_n=top_n)
        recommended_top_n = {user: [movie for movie, rating in movies] for user, movies in recommendations.items()}
        
        # Printing recommended_top_n for the first nb_users_to_print users
        print(f"\n       Recommendations (Top {top_n}) for {nb_users_to_print} Users:")
        for idx, (user, movies) in enumerate(recommended_top_n.items()):
            if idx >= nb_users_to_print:
                break
            print(f"          User {user}: {movies}")

        print()
        # Calculating Precision and Recall based on top n
        precision_recall = calculate_precision_recall(actual_top_n, recommended_top_n)

        # Asking user to select up to 5 users to view recall
        selected_users = get_nb_users_precision_recall(precision_recall, metric_type='recall', max_users=5)
        
        # Printing recall for selected users
        if selected_users:
            print_recall_selected(precision_recall, selected_users)
        else:
            print("       No users selected for recall display.")
        
        # Calculating and print average recall for all users
        total_recall = sum(metrics['recall'] for metrics in precision_recall.values())
        avg_recall = total_recall / len(precision_recall) if precision_recall else 0.0
        print(f"\n       Average Recall across all users: {avg_recall:.2f}")
    
    else: # eval_metric == "all"
        print(f"       Mean Absolute Error (MAE): {mae:.4f}")
        print(f"       Root Mean Squared Error (RMSE): {rmse:.4f}")
        top_n = get_top_n()
        print()
        # Extracting actual top n movies per user from the original dataset
        actual_top_n = get_top_n_actual(all_ratings, top_n=top_n)
        
        # Asking user how many users' data to print
        while True:
            try:
                nb_users_to_print = int(input(f"    G. How many users top {top_n} movies would you like to see? (1-5): ").strip())
                if 1 <= nb_users_to_print <= 5:
                    break
                else:
                    print("       Please enter a number between 1 and 5.")
            except ValueError:
                print("       Invalid input. Please enter a numerical value.")
        
        # Printing actual_top_n for the first nb_users_to_print users
        print(f"\n       Actual Top {top_n} Movies for {nb_users_to_print} Users:")
        for idx, (user, movies) in enumerate(actual_top_n.items()):
            if idx >= nb_users_to_print:
                break
            print(f"          User {user}: {movies}")

        # Generating Recommendations by combining train and predicted ratings
        recommendations = recommend_movies(train_ratings, predicted_ratings, top_n=top_n)
        recommended_top_n = {user: [movie for movie, rating in movies] for user, movies in recommendations.items()}
        
        # Printing recommended_top_n for the first nb_users_to_print users
        print(f"\n       Recommendations (Top {top_n}) for {nb_users_to_print} Users:")
        for idx, (user, movies) in enumerate(recommended_top_n.items()):
            if idx >= nb_users_to_print:
                break
            print(f"          User {user}: {movies}")
        
        print()
        # Calculating Precision and Recall based on top n
        precision_recall = calculate_precision_recall(actual_top_n, recommended_top_n)

        # Asking user to select up to 5 users to view both precision and recall
        selected_users = get_nb_users_precision_recall(precision_recall, metric_type='precision and recall', max_users=5)
        
        # Printing precision and recall for selected users
        if selected_users:
            print_precision_selected(precision_recall, selected_users)
            print_recall_selected(precision_recall, selected_users)
        else:
            print("       No users selected for precision and recall display.")
        
        # Calculating and print average precision and recall for all users
        total_precision = sum(metrics['precision'] for metrics in precision_recall.values())
        avg_precision = total_precision / len(precision_recall) if precision_recall else 0.0
        total_recall = sum(metrics['recall'] for metrics in precision_recall.values())
        avg_recall = total_recall / len(precision_recall) if precision_recall else 0.0
        print(f"\n       Average Precision across all users: {avg_precision:.2f}")
        print(f"       Average Recall across all users: {avg_recall:.2f}")

    print("\n ---—————————---—————————---————————— End of the Program  ---—————————---——————————————————--- \n\n")

# ------------------------ END OF MAIN EXECUTION FUNCTION ------------------------

if __name__ == "__main__":
    main()